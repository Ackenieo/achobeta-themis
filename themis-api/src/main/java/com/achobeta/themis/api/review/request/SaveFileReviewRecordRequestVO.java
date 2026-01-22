package com.achobeta.themis.api.review.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 请求体：保存文件审查记录
 */
@Data
public class SaveFileReviewRecordRequestVO {

    /**
     * 本地保存路径
     */
   /* @NotBlank(message = "本地文件路径不能为空")
    private String filePath;*/

    /**
     * 审查结果内容
     */
    @NotBlank(message = "审查结果内容不能为空")
    private String reviewContent;

    /**
     * 原始文件名
     */
    @NotBlank(message = "文件名不能为空")
    private String fileName;

    /**
     * 关联的对话ID，可选
     */
    private String conversationId;

    /**
     * 指定记录ID（可选，用于覆盖）
     */
    private String recordId;
}
