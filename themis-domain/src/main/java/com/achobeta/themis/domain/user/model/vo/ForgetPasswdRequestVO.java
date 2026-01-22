package com.achobeta.themis.domain.user.model.vo;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 忘记密码请求VO
 * @author AckenieoT
 * @date 2025/10/30 上午10:20
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForgetPasswdRequestVO {
        @NotBlank(message = "手机号不能为空")
        @Size(min = 11, max = 11, message = "手机号长度必须为11位")
        private String phone;

        @NotBlank(message = "未输入验证码")
        private String verifyCode;

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, message = "密码长度至少6个字符")
        private String newPassword;
}
