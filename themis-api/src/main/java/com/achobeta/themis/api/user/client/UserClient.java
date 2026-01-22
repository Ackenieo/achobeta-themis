package com.achobeta.themis.api.user.client;

import com.achobeta.themis.api.user.response.UserInfoResponse;
import com.achobeta.themis.common.ApiResponse;

/**
 * 提供给其他模块调用的接口
 * @author Aseubel
 * @date 2025/10/27 上午2:17
 */
public interface UserClient {
    ApiResponse<UserInfoResponse> getUserInfo(Long userId);
}
