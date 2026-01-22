package com.achobeta.themis.domain.user.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.achobeta.themis.common.exception.BusinessException;
import com.achobeta.themis.common.redis.service.RedissonService;
import com.achobeta.themis.domain.user.service.IVerifyCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;

/**
 * 验证码服务实现类
 * @author AckenieoT
 * @date 2025/10/30 上午10:20
 */

@Slf4j
@RequiredArgsConstructor
@Service
public class VerifyCodeServiceImpl implements IVerifyCodeService {
    private final RedissonService redissonService;
    
    // 验证码发送间隔（秒）
    private static final long SEND_INTERVAL_SECONDS = 60;
    
    // 验证码键前缀
    private static final String VERIFY_CODE_KEY_PREFIX = "verify_code:";
    
    // 发送时间键前缀
    private static final String SEND_TIME_KEY_PREFIX = "verify_code_send_time:";
    
    // 每天最大发送次数（设置为较大值或Integer.MAX_VALUE表示不限制）
    private static final int MAX_SEND_COUNT_PER_DAY = 999;
    
    // 每天发送次数键前缀
    private static final String SEND_COUNT_KEY_PREFIX = "verify_code_send_count:";

    @Override
    public String generateAndStoreCode(String phone, Duration duration) {
        // 生成验证码
        String verifyCode = RandomUtil.randomNumbers(6);
        log.info("生成验证码，手机号：{}", phone);
        String verifyCodeKey = VERIFY_CODE_KEY_PREFIX + phone;
        redissonService.setValue(verifyCodeKey, verifyCode, duration.toMillis());
        
        // 记录发送时间
        String sendTimeKey = SEND_TIME_KEY_PREFIX + phone;
        redissonService.setValue(sendTimeKey, String.valueOf(System.currentTimeMillis()), 
                Duration.ofSeconds(SEND_INTERVAL_SECONDS).toMillis());
        
        // 增加每天发送次数
        incrementDailySendCount(phone);
        
        return verifyCode;
    }

    @Override
    public void sendVerifyCode(String phone, String code) throws Exception {

            String url = "https://push.spug.cc/sms/CcAZlcoqQaCkqzPhyfoRTg";
        String json = String.format("{\"name\":\"验证码\",\"code\":\"%s\",\"to\":\"%s\"}", code, phone);

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }

        log.info("发送验证码到手机号：{}，验证码：{}", phone, code);
        // 您可以在这里添加实际的发送逻辑
    }
    
    @Override
    public boolean verifyCode(String phone, String code) {
        if (phone == null || code == null) {
            log.warn("验证验证码失败：手机号或验证码为空");
            return false;
        }
        
        String verifyCodeKey = VERIFY_CODE_KEY_PREFIX + phone;
        String storedCode = redissonService.getValue(verifyCodeKey);
        
        if (storedCode == null) {
            log.warn("验证码不存在或已过期，手机号：{}", phone);
            return false;
        }
        
        boolean isValid = storedCode.equals(code);
        if (isValid) {
            log.info("验证码验证成功，手机号：{}", phone);
            // 验证成功后删除验证码
            redissonService.remove(verifyCodeKey);
        } else {
            log.warn("验证码验证失败，手机号：{}", phone);
        }
        
        return isValid;
    }
    
    @Override
    public boolean canSendCode(String phone) {
        // 检查发送时间间隔
        String sendTimeKey = SEND_TIME_KEY_PREFIX + phone;
        String lastSendTime = redissonService.getValue(sendTimeKey);
        
        if (lastSendTime != null) {
            long remainingSeconds = getRemainingSeconds(phone);
            if (remainingSeconds > 0) {
                log.warn("验证码发送过于频繁，手机号：{}，需等待{}s", phone, remainingSeconds);
                return false;
            }
        }
        
        // 检查每天发送次数
        int dailyCount = getDailySendCount(phone);
        if (dailyCount >= MAX_SEND_COUNT_PER_DAY) {
            log.warn("今日验证码发送次数已达上限，手机号：{}，次数：{}", phone, dailyCount);
            return false;
        }
        
        return true;
    }
    
    @Override
    public long getRemainingSeconds(String phone) {
        String sendTimeKey = SEND_TIME_KEY_PREFIX + phone;
        String lastSendTime = redissonService.getValue(sendTimeKey);
        
        if (lastSendTime == null) {
            return 0;
        }
        
        try {
            long lastTime = Long.parseLong(lastSendTime);
            long elapsedSeconds = (System.currentTimeMillis() - lastTime) / 1000;
            long remaining = SEND_INTERVAL_SECONDS - elapsedSeconds;
            return Math.max(0, remaining);
        } catch (NumberFormatException e) {
            log.error("解析发送时间失败，手机号：{}", phone, e);
            return 0;
        }
    }
    
    /**
     * 获取今日发送次数
     */
    private int getDailySendCount(String phone) {
        String countKey = SEND_COUNT_KEY_PREFIX + phone;
        String count = redissonService.getValue(countKey);
        if (count == null) {
            return 0;
        }
        try {
            return Integer.parseInt(count);
        } catch (NumberFormatException e) {
            log.error("解析发送次数失败，手机号：{}", phone, e);
            return 0;
        }
    }
    
    /**
     * 增加今日发送次数
     */
    private void incrementDailySendCount(String phone) {
        String countKey = SEND_COUNT_KEY_PREFIX + phone;
        int currentCount = getDailySendCount(phone);
        // 设置过期时间为第二天凌晨0点
        long midnightMillis = getMidnightMillis();
        redissonService.setValue(countKey, String.valueOf(currentCount + 1), midnightMillis);
    }
    
    /**
     * 获取到第二天凌晨0点的毫秒数
     */
    private long getMidnightMillis() {
        long now = System.currentTimeMillis();
        long oneDayMillis = 24 * 60 * 60 * 1000;
        long todayStartMillis = now - (now % oneDayMillis) + 8 * 60 * 60 * 1000; // UTC+8
        long tomorrowStartMillis = todayStartMillis + oneDayMillis;
        return tomorrowStartMillis - now;
    }
}
