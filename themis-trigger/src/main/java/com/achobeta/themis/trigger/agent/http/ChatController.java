package com.achobeta.themis.trigger.agent.http;

import com.achobeta.themis.common.ApiResponse;
import com.achobeta.themis.common.agent.service.IAiChatService;
import com.achobeta.themis.common.annotation.LoginRequired;
import com.achobeta.themis.common.exception.BusinessException;
import com.achobeta.themis.common.util.SecurityUtils;
import com.achobeta.themis.domain.chat.model.entity.ConversationMeta;
import com.achobeta.themis.api.chat.request.ChatRequestVO;
import com.achobeta.themis.domain.user.model.vo.QuestionTitleResponseVO;
import com.achobeta.themis.domain.review.service.IAdjudicatorService;
import com.achobeta.themis.domain.chat.service.IChatService;
import com.achobeta.themis.domain.chat.service.IConversationHistoryService;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@LoginRequired
public class ChatController {
    @Autowired
    @Qualifier("consulter")
    private IAiChatService consulterService;
    @Autowired
    @Qualifier("threadPoolExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    private final IAdjudicatorService adjudicatorService;

    private final IChatService chatService;

    @Autowired
    private ChatMemoryStore chatMemoryStore;

    @Autowired
    private IConversationHistoryService conversationHistoryService;
    /*
    * 拿到标题*/
    /*@GetMapping("/get-question-titles")
    public static  getQuestionTitles(@RequestParam (value = "title")) {

    }*/
    /**
     * Ai问答聊天（阻塞式，返回完整响应）
     * @param request
     * @return
     */
    @LoginRequired
    @PostMapping("/consult")
    public ApiResponse<String> chat(@Valid @RequestBody ChatRequestVO request) {
        try {
            String userId = SecurityUtils.getCurrentUserId();
            threadPoolTaskExecutor.execute(() -> {
                log.info("异步处理问题分类");
                try {
                    adjudicatorService.adjudicate(request.getUserType(), request.getConversationId(), request.getMessage());
                } catch (Exception e) {
                    log.error("问题分类失败", e);
                }
            });
            threadPoolTaskExecutor.execute(() -> {
                log.info("异步触碰并续期对话历史");
                try {
                    conversationHistoryService.touch(userId, request.getConversationId());
                } catch (Exception e) {
                    log.error("触碰对话历史失败", e);
                }
            });
            List<String> response = consulterService.chat(request.getConversationId(), request.getMessage()).collectList().block();
            String responseStr = String.join("", response);
            return ApiResponse.success(responseStr);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ai问答聊天失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * Ai问答聊天（流式响应，SSE）
     * @param request
     * @return 流式响应
     */
    @LoginRequired
    @PostMapping(value = "/consult/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@Valid @RequestBody ChatRequestVO request) {
        try {
            String userId = SecurityUtils.getCurrentUserId();
            
            // 异步处理问题分类
            threadPoolTaskExecutor.execute(() -> {
                log.info("异步处理问题分类");
                try {
                    adjudicatorService.adjudicate(request.getUserType(), request.getConversationId(), request.getMessage());
                } catch (Exception e) {
                    log.error("问题分类失败", e);
                }
            });
            
            // 异步触碰并续期对话历史
            threadPoolTaskExecutor.execute(() -> {
                log.info("异步触碰并续期对话历史");
                try {
                    conversationHistoryService.touch(userId, request.getConversationId());
                } catch (Exception e) {
                    log.error("触碰对话历史失败", e);
                }
            });
            
            log.info("开始流式响应，conversationId: {}, userId: {}", request.getConversationId(), userId);
            
            // 直接返回 Flux 流，Spring 会自动处理 SSE 传输
            return consulterService.chat(request.getConversationId(), request.getMessage())
                    .doOnNext(chunk -> log.debug("发送流式数据块: {}", chunk))
                    .doOnComplete(() -> log.info("流式响应完成，conversationId: {}", request.getConversationId()))
                    .doOnError(error -> log.error("流式响应错误，conversationId: {}", request.getConversationId(), error))
                    .onErrorResume(error -> {
                        log.error("流式响应发生错误", error);
                        return Flux.just("错误: " + error.getMessage());
                    });
                    
        } catch (BusinessException e) {
            log.error("流式聊天业务异常", e);
            return Flux.error(e);
        } catch (Exception e) {
            log.error("流式聊天失败", e);
            return Flux.error(new RuntimeException("流式聊天失败: " + e.getMessage()));
        }
    }


    /**
     * 新建对话，返回新 conversationId
     * */
    @PostMapping("/newOr")
    public ApiResponse<String> newOr() {

        try {
            return ApiResponse.success(UUID.randomUUID().toString());
        } catch (Exception e) {
            log.error("新建对话失败", e);
            return ApiResponse.error("新建对话失败: " + e.getMessage());
        }
    }
    /**
     * 新建对话：归档当前，返回新 conversationId
     */
    @PostMapping("/new")
    public ApiResponse<String> newConversation(
            // @RequestParam("userId") Long userId,
            @RequestParam(value = "title",defaultValue = "未命名") String title,
            @RequestParam(value = "currentConversationId") String currentConversationId
    ) {

        try {
            String userId = SecurityUtils.getCurrentUserId();
            String newId = conversationHistoryService.startNewConversation(userId, currentConversationId,title);
            return ApiResponse.success(newId);
        } catch (Exception e) {
            log.error("新建对话失败", e);
            return ApiResponse.error("新建对话失败: " + e.getMessage());
        }
    }

    /**
     * 查询对话记录
     */
    @GetMapping("/history")
    public ApiResponse<List<ChatHistoryVO>> history(
            @RequestParam("conversationId") @NotBlank(message = "对话ID不能为空") String conversationId
    ) {
        try {
            List<ChatMessage> messages = chatMemoryStore.getMessages(conversationId);
            List<ChatHistoryVO> history = messages.stream()
                    .map(this::toHistory)
                    .collect(Collectors.toList());
            return ApiResponse.success(history);
        } catch (Exception e) {
            log.error("查询对话历史失败", e);
            return ApiResponse.error("查询对话历史失败: " + e.getMessage());
        }
    }

    /**
     * 查询当前用户的全部历史对话元信息
     */

    @GetMapping("/histories")
    public ApiResponse<List<ConversationMeta>> histories(
           // @RequestParam("userId") Long userId
    ) {

        try {
            String userId = SecurityUtils.getCurrentUserId();
            List<ConversationMeta> list = conversationHistoryService.listHistories(userId);
            return ApiResponse.success(list);
        } catch (Exception e) {
            log.error("查询用户历史对话失败", e);
            return ApiResponse.error("查询用户历史对话失败: " + e.getMessage());
        }
    }

    /**
     * 删除对话记录
     */
    @DeleteMapping("/history")
    public ApiResponse<Void> resetHistory(
            @RequestParam("conversationId") @NotBlank(message = "对话ID不能为空") String conversationId
    ) {
        try {
            chatMemoryStore.deleteMessages(conversationId);
            log.info("已重置对话历史，conversationId: {}", conversationId);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("重置对话历史失败", e);
            return ApiResponse.error("重置对话历史失败: " + e.getMessage());
        }
    }

    /**
     * 查询常问问题（二级标题）
     * @return
     */
    @GetMapping("/secondary_question_titles/{userType}")
    public ApiResponse<List<List<QuestionTitleResponseVO>>> searchQuestionTitles(@PathVariable("userType") @NotNull(message = "用户类型不能为空") Integer userType) {
        try {
            List<List<QuestionTitleResponseVO>> questionTitleDocuments = chatService.searchQuestionTitles(userType);
            return ApiResponse.success(questionTitleDocuments);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询常问问题失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChatHistoryVO {
        private String role;
        private String content;
        private LocalDateTime timestamp;
    }

    private ChatHistoryVO toHistory(ChatMessage message) {
        return new ChatHistoryVO(
                message.type().name(),
                resolveContent(message),
                LocalDateTime.now()
        );
    }

    private String resolveContent(ChatMessage message) {
        try {
            Method textMethod = message.getClass().getMethod("text");
            Object value = textMethod.invoke(message);
            if (value instanceof String text) {
                return text;
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }
        return message.toString();
    }

//    /**
//     * Ai改合同
//     */
//    @PostMapping("/contract_consult")
//    public ApiResponse<String> contractConsult(@Valid @RequestBody ChatRequestVO request) {
//        try {
//            List<String> response = consulterService.chat(request.getConversationId(), request.getMessage()).collectList().block();
//            String responseStr = String.join("", response);
//            chatService.consulterCorrect(request.getConversationId(), responseStr);
//            return ApiResponse.success(responseStr);
//        } catch (BusinessException e) {
//            throw e;
//        } catch (Exception e) {
//            log.error("用户登出失败", e);
//            return ApiResponse.error(e.getMessage());
//        }
//    }
    


}
