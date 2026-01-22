package com.achobeta.themis.domain.user.model.vo;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 前端登录请求对象
 * @author AckenieoT
 * @date 2025/10/30 上午10:20
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestVO {

    @NotBlank(message = "手机号不能为空")
    @Size(min = 11, max = 11, message = "手机号长度必须为11位")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度至少6个字符")
    private String password;

    @Min(value = 1, message = "用户类型只能为1或2")
    @Max(value = 2, message = "用户类型只能为1或2")
    private Integer userType;

    @NotBlank(message = "客户端ID不能为空")
    private String clientId;
}

