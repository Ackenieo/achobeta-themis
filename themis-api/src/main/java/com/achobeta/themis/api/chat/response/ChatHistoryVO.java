package com.achobeta.themis.api.chat.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <p>
 * 描述：
 * </p>
 *
 * @Author: ZGjie20
 * @version: 1.0.0
 */
@Data
@AllArgsConstructor
public  class ChatHistoryVO {
    private String role;
    private String content;
    private LocalDateTime timestamp;
}
