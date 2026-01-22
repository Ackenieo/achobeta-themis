package com.achobeta.themis.domain.user.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证响应VO
 * @author AckenieoT
 * @date 2025/10/30 上午10:20
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponseVO {
        private Long id;
        private String userId;
        private String username;
        private String accessToken;
        private String refreshToken;
}
