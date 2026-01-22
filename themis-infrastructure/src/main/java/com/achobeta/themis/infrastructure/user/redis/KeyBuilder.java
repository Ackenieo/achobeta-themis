package com.achobeta.themis.infrastructure.user.redis;

import static com.achobeta.themis.infrastructure.user.redis.RedisKey.*;

/**
 * @author Aseubel
 * @date 2025/6/28 下午9:22
 */
public class KeyBuilder {

    public static String smsCodeKey(String mobile) {
        return APP + REDIS_SMS_CODE_PREFIX + mobile;
    }

}
