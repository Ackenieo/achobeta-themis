package com.achobeta.themis.domain.user.service;

import java.time.Duration;

/**
 * 验证码服务接口
 * @author AckenieoT
 * @date 2025/10/30 上午10:20
 */

public interface IVerifyCodeService {
    /**
     * 生成并存储验证码
     * @param phone
     * @param duration
     * @return
     */
    String generateAndStoreCode(String phone, Duration duration);

    /**
     * 发送验证码
     * @param phone
     * @param code
     */
    void sendVerifyCode(String phone, String code) throws Exception;
    
    /**
     * 验证验证码是否正确
     * @param phone 手机号
     * @param code 验证码
     * @return true-验证成功，false-验证失败
     */
    boolean verifyCode(String phone, String code);
    
    /**
     * 检查是否可以发送验证码（频率限制）
     * @param phone 手机号
     * @return true-可以发送，false-需要等待
     */
    boolean canSendCode(String phone);
    
    /**
     * 获取下次可以发送验证码的剩余秒数
     * @param phone 手机号
     * @return 剩余秒数，0表示可以立即发送
     */
    long getRemainingSeconds(String phone);
}
