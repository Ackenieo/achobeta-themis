package com.achobeta.themis.domain.user.service;

import com.achobeta.themis.domain.user.model.vo.*;
import com.achobeta.themis.domain.user.model.UserModel;

/**
 * @author Aseubel
 * @date 2025/10/27 上午1:42
 */
public interface IUserService {

    /**
     * 根据用户ID获取用户信息
     * @param userModel
     * @return
     */
    UserModel getUserByUserId(UserModel userModel);

    /**
     * 根据用户ID获取用户信息
     * @param userId
     * @return
     */
    UserModel getUserInfo(Long userId);

    /**
     * 用户登录
     * @param request
     * @return
     */
     AuthResponseVO login(LoginRequestVO request);

    /**
     * 刷新访问令牌
     * @param refreshToken
     * @return
     */
    AuthResponseVO refreshToken(String refreshToken);

    /**
     * 忘记密码
     * @param request
     */
    void forgetPassword(ForgetPasswdRequestVO request);

    /**
     * 发送验证码
     * @param phone
     */
    void sendVerifyCode(String phone) throws Exception;

    /**
     * 用户登出
     * @param refreshToken
     */
    void logout(String refreshToken);

    /**
     * 全部登出
     * @param userId
     */
    void logoutAll(Long userId);

    /**
     * 修改密码
     * @param request
     */
    void changePassword(ChangePasswordRequestVO request);

    /**
     * 修改用户名
     * @param request
     */
    void changeUsername(ChangeUsernameRequestVO request);
}
