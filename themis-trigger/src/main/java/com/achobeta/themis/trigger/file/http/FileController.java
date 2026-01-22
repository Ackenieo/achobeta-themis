package com.achobeta.themis.trigger.file.http;

import com.achobeta.themis.common.ApiResponse;
import com.achobeta.themis.common.agent.service.IAiChatService;
import com.achobeta.themis.common.annotation.LoginRequired;
import com.achobeta.themis.common.exception.BusinessException;
import com.achobeta.themis.common.util.SecurityUtils;
import com.achobeta.themis.domain.user.model.UserModel;
import com.achobeta.themis.api.review.request.ReviewRequest;
import com.achobeta.themis.api.review.response.ReviewResult;
import com.achobeta.themis.api.review.response.fileReturn;
import com.achobeta.themis.api.chat.response.ChatHistoryVO;
import com.achobeta.themis.api.review.request.DownLoadFileRequest;
import com.achobeta.themis.api.review.response.FileReviewRecordVO;
import com.achobeta.themis.api.review.request.SaveFileReviewRecordRequestVO;
import com.achobeta.themis.domain.review.service.IFileReviewHistoryService;
import com.achobeta.themis.domain.review.service.IFileService;
import com.achobeta.themis.domain.user.service.IUserService;
import dev.langchain4j.data.message.ChatMessage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@LoginRequired
@Slf4j
@Validated
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final IUserService userService;

    @Autowired
    private IAiChatService chatService;

    private final IFileReviewHistoryService fileReviewHistoryService;

    private final IFileService fileService;


    private static final int MAX_TEXT_LENGTH = 20000;

    private static final Map<String, MediaType> CONTENT_TYPE_MAP = new HashMap<>();

    static {
        CONTENT_TYPE_MAP.put("application/pdf", MediaType.APPLICATION_PDF);
        CONTENT_TYPE_MAP.put("docx", MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
    }

    /**
     * 提交内容审查
     *
     */
    @PostMapping(value = "/review")
    public ApiResponse<ReviewResult> review(
            @RequestBody ReviewRequest request
    ) {
        String text = request.getText();
        String conversationId = request.getConversationId();
        String fileName = request.getFileName();
        try {
            if (conversationId == null || conversationId.isBlank()) {
                throw new BusinessException("对话ID不能为空");
            }
            if (text == null || text.isBlank()) {
                throw new BusinessException("未能从聊天框中提取到文本，请检查输入内容");
            }
            if (text.length() > MAX_TEXT_LENGTH) {
                text = text.substring(0, MAX_TEXT_LENGTH);
            }

            Long id = SecurityUtils.getId();
            UserModel userModel = userService.getUserInfo(id);
            Integer userType = userModel.getUser().getUserType();
            String prompt = buildReviewPrompt(fileName, text, userType);
            Flux<String> stream = chatService.chat(conversationId, prompt);
            List<String> chunks = stream.collectList().block();
            String review = String.join("", chunks);

            return ApiResponse.success(
                    new ReviewResult(conversationId, fileName, review));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("对话失败", e);
            return ApiResponse.error(e.getMessage());

        }
    }

    /**
     * 解析文件中的文字内容并返回
     */
    @PostMapping(value = "/upAndwrite", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<fileReturn> upAndwrite(
            @RequestParam("file") MultipartFile file
            // @RequestParam("conversationId") @NotBlank(message = "对话ID不能为空") String conversationId
    ) throws TikaException, IOException, SAXException {
        try {
            if (file.isEmpty()) {
                throw new BusinessException("上传文件不能为空");
            }

           /* if (conversationId == null || conversationId.isBlank()) {
                throw new BusinessException("对话ID不能为空");
            }*/

            // String savedPath = saveToLocal(file, conversationId);
            fileReturn fileReturn = new fileReturn();
            fileReturn.setFileName(file.getOriginalFilename());
            fileReturn.setText(extractText(file));


            return ApiResponse.success(fileReturn);
            // return  text;
           /* if (text == null || text.isBlank()) {
                throw new BusinessException("未能从文件中提取到文本，请检查文件内容或格式");
            }
            if (text.length() > MAX_TEXT_LENGTH) {
                text = text.substring(0, MAX_TEXT_LENGTH);
            }

            String prompt = buildReviewPrompt(file.getOriginalFilename(), text);
            Flux<String> stream = chatService.chat(conversationId, prompt);
            List<String> chunks = stream.collectList().block();
            String review = String.join("", chunks);

            return ApiResponse.success(
                    new ReviewResult(conversationId, savedPath, file.getOriginalFilename(), review)
            );*/
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("文件解析回写失败", e);
            return ApiResponse.error(e.getMessage());

        }
    }

    /**
     * 获取对话ID
     *
     */
    @PostMapping("/review/getId")
    private String getConversationId() {
        return UUID.randomUUID().toString();
    }

    /**
     * 保存审查记录
     *
     * @return 新生成的记录ID
     */
    @PostMapping("/review/record")
    public ApiResponse<String> saveReviewRecord(
            @Valid @RequestBody SaveFileReviewRecordRequestVO request,
            @RequestParam("flag") boolean flag

            //  @RequestParam("userId") Long userId
    ) {
        if (!flag) {
            return ApiResponse.success("不保存");
        }
        try {
            String userId = SecurityUtils.getCurrentUserId();
            String recordId = request.getRecordId();
            if (recordId == null || recordId.isBlank()) {
                throw new BusinessException("记录ID不能为空");
            }
            long currentTime = System.currentTimeMillis();

            IFileReviewHistoryService.ReviewRecord reviewRecord =
                    new IFileReviewHistoryService.ReviewRecord(
                            recordId,
                            request.getFileName(),
                            request.getReviewContent(),
                            currentTime,
                            currentTime
                    );

            fileReviewHistoryService.saveReviewRecord(userId, reviewRecord);
            log.info("审查记录已保存，recordId: {}, userId: {}", recordId, userId);
            return ApiResponse.success("审查记录已保存", recordId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("保存审查记录失败", e);
            return ApiResponse.error("保存审查记录失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的文件审查记录列表
     *
     * @return 审查记录列表
     */
    @GetMapping("/review/records")
    public ApiResponse<FileReviewRecordVO.ReviewRecordListVO> getReviewRecords(
            //  @RequestParam("userId") Long userId
    ) {
        try {
            String userId = SecurityUtils.getCurrentUserId();
            List<IFileReviewHistoryService.ReviewRecord> records =
                    fileReviewHistoryService.getUserReviewRecords(userId);

            List<FileReviewRecordVO> recordVOs = records.stream()
                    .map(record -> new FileReviewRecordVO(
                            record.getRecordId(),
                            record.getFileName(),
                            record.getReviewContent(),
                            record.getCreateTime(),
                            record.getUpdateTime()
                    ))
                    .collect(Collectors.toList());

            FileReviewRecordVO.ReviewRecordListVO result =
                    new FileReviewRecordVO.ReviewRecordListVO(recordVOs);
            return ApiResponse.success(result);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取审查记录列表失败", e);
            return ApiResponse.error("获取审查记录列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定的审查记录详情
     *
     * @param recordId 记录ID
     * @return 审查记录详情
     */
    @GetMapping("/review/record")
    public ApiResponse<FileReviewRecordVO> getReviewRecord(
            @RequestParam("recordId") @NotBlank(message = "记录ID不能为空") String recordId
            // @RequestParam("userId") Long userId
    ) {
        try {
            String userId = SecurityUtils.getCurrentUserId();
            IFileReviewHistoryService.ReviewRecord record =
                    fileReviewHistoryService.getReviewRecord(userId, recordId);

            if (record == null) {
                return ApiResponse.error("审查记录不存在或无权访问");
            }

            FileReviewRecordVO recordVO = new FileReviewRecordVO(
                    record.getRecordId(),
                    record.getFileName(),

                    record.getReviewContent(),
                    record.getCreateTime(),
                    record.getUpdateTime()
            );

            return ApiResponse.success(recordVO);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取审查记录详情失败", e);
            return ApiResponse.error("获取审查记录详情失败: " + e.getMessage());
        }
    }

    /**
     * 删除审查记录
     *
     * @param recordId 记录ID
     * @return
     */
    @DeleteMapping("/review/record")
    public ApiResponse<Void> deleteReviewRecord(
            @RequestParam("recordId") @NotBlank(message = "记录ID不能为空") String recordId
    ) {
        try {
            String userId = SecurityUtils.getCurrentUserId();
            fileReviewHistoryService.deleteReviewRecord(userId, recordId);
            log.info("已删除审查记录，recordId: {}", recordId);
            return ApiResponse.success(null);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除审查记录失败", e);
            return ApiResponse.error("删除审查记录失败: " + e.getMessage());
        }
    }

    /**
     * 下载文件
     *
     * @param request 下载文件请求参数
     * @return 下载url
     */
    @LoginRequired
    @PostMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestBody DownLoadFileRequest request) {
        try {
            Resource resource = fileService.generateDownloadResource(request.getIsChange(), request.getConversationId(), request.getContentType(), fileService.contentTransform(request.getContent()));
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + URLEncoder.encode(resource.getFilename() + request.getContentType(), "UTF-8") + "\"") // 中文文件名编码
                    .contentType(CONTENT_TYPE_MAP.getOrDefault(request.getContentType(), MediaType.APPLICATION_OCTET_STREAM))
                    .body(resource);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("下载文件失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }


//    private String saveToLocal(MultipartFile file, String conversationId) throws IOException {
//        Path base = Paths.get("D:\\A\\ruku\\upload");
//        Path dir = base.resolve(sanitize(conversationId));
//        Files.createDirectories(dir);
//        String ts = LocalDateTime.now().format(TS_FMT);
//        String original = file.getOriginalFilename();
//        String safeName = ts + "_" + sanitize(original == null ? "unknown" : original);
//        Path dest = dir.resolve(safeName);
//        try (InputStream in = file.getInputStream()) {
//            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
//        }
//        log.info("文件已保存到本地：{}", dest.toAbsolutePath());
//        return dest.toAbsolutePath().toString();
//    }

    private String extractText(MultipartFile file) throws IOException, TikaException, SAXException {
        try (InputStream is = file.getInputStream()) {
            Tika tika = new Tika();
            String text = tika.parseToString(is);
            return text != null ? text.trim() : null;
        }
    }

    private String buildReviewPrompt(String filename, String content, Integer userType) {
        String systemPrompt = "你是一名中国劳动法与劳动合规的专业助手，现需审查以下合同文本并以JSON格式输出结果，具体规则如下：" +
                "\n\n一、分级要求\n仅呈现存在的分级(集合名称)；若“\"illegal\"”“\"missing\"”“\"risk\"”三类均不存在，则输出 \"legal\"(空集合)" +
                "\n1. \"illegal\"：明确违反现行中国劳动法律法规或强制性标准的条款或表述\n" +
                "2. \"missing\"：法律法规或监管实践中普遍要求但合同中缺失的关键条款（如必备要素、程序性条款）\n" +
                "3. \"risk\"：可能引发争议或合规风险，但未必直接违法的条款或表述\n\n" +
                "二、每个问题项的固定字段\\n每个问题项为JSON对象，包含以下字段（按要求填写，无对应内容时按规则处理）：\n" +
                "- \"startIndex\"：整数类型，填写合同原句在文本中首个字符的位置索引（从0开始计数）\n" +
                "- \"endIndex\"：整数类型，填写合同原句在文本中最后一个字符的位置索引,含标点（从0开始计数）\n" +
                "- \"originalSentence\"：字符串类型，填写原文精确片段；缺失必备项填写\"无对应原文，合同未约定\"\n" +
                "- \"relatedClauses\"：字符串类型，简要给出对应的法律/法规/司法解释（可概述条文要点）\n" +
                "- \"suggestion\"：字符串类型，根据当前用户类型，填写站在个人用户 或 企业用户视角的修改建议；\n" +
                "三、JSON输出格式要求\\n" +
                "1. 整体为单个JSON对象，顶级字段仅包含存在的分级（\"illegal\"\"missing\"\"risk\"），无则不出现；三类均无时仅保留\"legal\"字段\n" +
                "2. 各分级字段的值为数组类型，数组内每个元素为一个符合上述固定字段要求的问题项JSON对象\n" +
                "3. 分级字段按\\\"illegal\"→\"missing\"→\"risk\"的顺序排列，数组内问题项按合同文本顺序（以原句位置索引为准）或重要性排序\n\n" +
                "四、基础信息\n" +
                "- 文件名：{filename}（filename为null时填写\"未知\"）\n" +
                "- 当前用户类型：{usertype}\n\n待审查文本：\n{contract_text}\n\n" +
                "注：使用时{filename}、{usertype}、{contract_text}替换为实际值，输出仅保留JSON数据，无任何额外冗余内容，确保JSON格式合法可解析。\n" +
                "其中：\n" +
                "filename：" + (filename == null ? "未知" : filename) + "\n" +
                "userType：" + (userType == 1 ? "个人用户" : "企业用户") + "\n" +
                "contractText：" + content + "\n";
        return systemPrompt;
    }

    private String sanitize(String s) {
        return s.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
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


}

