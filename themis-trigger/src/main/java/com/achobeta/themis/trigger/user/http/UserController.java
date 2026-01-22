package com.achobeta.themis.trigger.user.http;

import com.achobeta.themis.api.user.client.UserClient;
import com.achobeta.themis.api.user.response.UserInfoResponse;
import com.achobeta.themis.common.ApiResponse;
import com.achobeta.themis.common.annotation.LoginRequired;
import com.achobeta.themis.common.exception.BusinessException;
import com.achobeta.themis.domain.user.model.vo.*;
import com.achobeta.themis.domain.user.model.UserModel;
import com.achobeta.themis.domain.user.service.IUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 前端http服务请求控制器
 *
 * @author Aseubel
 * @date 2025/10/27 上午2:18
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController implements UserClient {

    private final IUserService userService;

    /**
     * 获取当前用户信息
     */
    @LoginRequired
    @GetMapping("/info")
    public ApiResponse<UserInfoResponse> getUserInfo(
            @RequestParam("userId") Long userId) {
        try {
            UserModel model = userService.getUserInfo(userId);
            UserInfoResponse userInfo = ofUserInfoResponse(model);
            return ApiResponse.success(userInfo);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取用户信息失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 用户登录
     * @param request
     * @return
     */
    @PostMapping("/login")
    public ApiResponse<AuthResponseVO> login(@Valid @RequestBody LoginRequestVO request) {
        try {
            AuthResponseVO response = userService.login(request);
            return ApiResponse.success("登录成功", response);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("用户登录失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 登出用户
     * @param refreshToken
     * @return
     */
    @LoginRequired
    @DeleteMapping("/logout")
    public ApiResponse<String> logout(@NotBlank(message = "刷新令牌不能为空") @RequestParam("refreshToken") String refreshToken) {
        try {
            userService.logout(refreshToken);
            return ApiResponse.success("登出成功");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("用户登出失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 登出所有设备
     * @param userId
     * @return
     */
    @LoginRequired
    @DeleteMapping("/logout-all")
    public ApiResponse<String> logoutAll(@NotNull(message = "用户id不能为空") @RequestParam("userId") Long userId) {
        try {
            userService.logoutAll(userId);
            return ApiResponse.success("批量注销成功");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("批量注销用户所有登录会话失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * Refresh token 令牌刷新
     * @param refreshToken
     * @return
     */
    @PostMapping("/refresh-token")
    public ApiResponse<AuthResponseVO> refreshToken(@NotBlank(message = "刷新令牌不能为空") @RequestParam("refreshToken") String refreshToken) {
        try {
            AuthResponseVO response = userService.refreshToken(refreshToken);
            return ApiResponse.success("刷新成功", response);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("刷新令牌失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 发送验证码
     * @param phone
     * @return
     */
    @PostMapping("/send-verify-code")
    public ApiResponse<String> sendVerifyCode(@NotBlank(message = "手机号码不能为空") @RequestParam("phone") String phone) {
        try {
            userService.sendVerifyCode(phone);
            return ApiResponse.success("验证码发送成功");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("验证码发送失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 忘记密码
     * @param request
     * @return
     */
    @PatchMapping("/forget")
    public ApiResponse<String> forgetPassword(@Valid @RequestBody ForgetPasswdRequestVO request) {
        try {
            userService.forgetPassword(request);
            return ApiResponse.success("密码修改成功");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("密码修改失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 修改用户密码
     * @param request
     * @return
     */
     @LoginRequired
     @PatchMapping("/change-password")
     public ApiResponse<String> changePassword(@Valid @RequestBody ChangePasswordRequestVO request) {
        try {
            userService.changePassword(request);
            return ApiResponse.success("密码修改成功");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("密码修改失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 修改用户名
     * @param request
     */
     @LoginRequired
     @PatchMapping("/change-username")
     public ApiResponse<String> changeUsername(@Valid @RequestBody ChangeUsernameRequestVO request) {
        try {
            userService.changeUsername(request);
            return ApiResponse.success("用户名修改成功");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("用户名修改失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 转换用户模型为用户信息响应
     * @param model
     * @return
     */
    private UserInfoResponse ofUserInfoResponse(UserModel model) {
        UserInfoResponse userInfo = UserInfoResponse.builder()
                .id(model.getUser().getId())
                .username(model.getUser().getUsername())
                .phone(model.getUser().getPhone())
                .build();
        return userInfo;
    }

}
