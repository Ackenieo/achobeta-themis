package com.achobeta.themis.api.chat.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatRequestVO {
    @NotNull(message = "用户ID不能为空")
    private Long id;
    @NotNull(message = "用户类型不能为空")
    @Max(value = 1, message = "用户类型只能是0或1")
    @Min(value = 0, message = "用户类型只能是0或1")
    private Integer userType;
    @NotBlank(message = "对话ID不能为空")
    private String conversationId;
    @NotBlank(message = "消息内容不能为空")
    private String message;
}