package com.achobeta.themis.api.review.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DownLoadFileRequest {
    @NotBlank(message = "是否变更不能为空")
    Boolean isChange;
    @NotBlank(message = "会话id不能为空")
    String conversationId;
    @NotBlank(message = "文件类型不能为空")
    String contentType;
    @NotBlank(message = "文件内容不能为空")
    String content;
}
