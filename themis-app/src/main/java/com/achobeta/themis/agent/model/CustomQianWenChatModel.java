//import dev.langchain4j.model.chat.ChatModel;
//import dev.langchain4j.model.chat.response.ChatResponse;
//import dev.langchain4j.model.chat.request.ChatRequest;
//import dev.langchain4j.data.message.ChatMessage;
//import dev.langchain4j.model.input.Prompt;
//import dev.langchain4j.model.input.PromptTemplate;
//import dev.langchain4j.model.input.structured.StructuredPrompt;
//import com.alibaba.dashscope.aigc.generation.Generation;
//import com.alibaba.dashscope.aigc.generation.GenerationParam;
//import com.alibaba.dashscope.aigc.generation.GenerationResult;
//import com.alibaba.dashscope.common.Message;
//import com.alibaba.dashscope.common.Role;
//import com.alibaba.dashscope.exception.ApiException;
//import com.alibaba.dashscope.exception.InputRequiredException;
//import com.alibaba.dashscope.exception.NoApiKeyException;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
///**
// * 自定义ChatModel：基于langchain4j 1.0.1，封装DashScope SDK实现联网搜索
// */
//@Component("QianWenChatModel")
//public class CustomQianWenChatModel implements ChatModel {
//
//    @Value("${langchain4j.dashscope.api-key}")
//    private String apiKey;
//
//    @Value("${langchain4j.dashscope.model:qwen-max}")
//    private String modelName;
//
//    @Override
//    public ChatResponse call(Prompt prompt) {
//        try {
//            // 1. 转换langchain4j Prompt为DashScope Message
//            List<Message> dashMessages = convertToDashScopeMessages(prompt);
//
//            // 2. 配置联网搜索参数（适配低版本SDK）
//            Map<String, Object> searchOptions = new HashMap<>();
//            searchOptions.put("forcedSearch", false); // 低版本参数名
//            searchOptions.put("timeRange", "1d"); // 低版本驼峰命名
//            searchOptions.put("searchResultCount", 5);
//
//            // 3. 构建DashScope调用参数
//            GenerationParam param = GenerationParam.builder()
//                    .model(modelName)
//                    .messages(dashMessages)
//                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
//                    .enableSearch(true) // 启用联网搜索
//                    .searchOptions(searchOptions)
//                    .topP(0.8)
//                    .temperature(0.7f)
//                    .apiKey(apiKey) // 关键：设置API Key
//                    .build();
//
//            // 4. 调用DashScope SDK
//            Generation gen = new Generation();
//            gen.apiKey(apiKey); // 兼容低版本的API Key设置
//            GenerationResult result = gen.call(param);
//
//            // 5. 转换结果为langchain4j ChatResponse
//            return convertToLangChain4jResponse(result);
//
//        } catch (NoApiKeyException e) {
//            throw new RuntimeException("未配置DashScope API Key", e);
//        } catch (InputRequiredException e) {
//            throw new RuntimeException("输入参数缺失", e);
//        } catch (ApiException e) {
//            throw new RuntimeException("千问模型调用失败：" + e.getMessage(), e);
//        } catch (Exception e) {
//            throw new RuntimeException("模型调用异常", e);
//        }
//    }
//
//    /**
//     * 转换langchain4j Prompt到DashScope消息列表
//     */
//    private List<Message> convertToDashScopeMessages(Prompt prompt) {
//        List<Message> messages = new ArrayList<>();
//
//        // 处理系统消息
//        messages.add(Message.builder()
//                .role(Role.SYSTEM.getValue())
//                .content("你是一个有帮助的助手，使用联网搜索获取最新信息")
//                .build());
//
//        // 处理用户消息
//        messages.add(Message.builder()
//                .role(Role.USER.getValue())
//                .content(prompt.text())
//                .build());
//
//        return messages;
//    }
//
//    /**
//     * 转换DashScope结果到langchain4j ChatResponse
//     */
//    private ChatResponse convertToLangChain4jResponse(GenerationResult result) {
//        if (result == null || result.getOutput() == null || result.getOutput().getChoices().isEmpty()) {
//            return ChatResponse.builder()
//                    .id("empty-response")
//                    .build();
//        }
//
//        Message aiMessage = result.getOutput().getChoices().get(0).getMessage();
//
//        return ChatResponse.builder()
//                .id(result.getRequestId())
//                .addMessage(ChatMessage.aiMessage(aiMessage.getContent()))
//                // 如果需要展示搜索来源，可以添加工具调用结果
//                .addToolExecutionResult(ToolExecutionResult.builder()
//                        .toolName("web-search")
//                        .success(true)
//                        .build())
//                .build();
//    }
//
//    // 兼容langchain4j的其他方法
//    @Override
//    public ChatResponse call(ChatRequest chatRequest) {
//        // 简单实现：转换ChatRequest到Prompt
//        return call(Prompt.of(chatRequest.userMessage().text()));
//    }
//}