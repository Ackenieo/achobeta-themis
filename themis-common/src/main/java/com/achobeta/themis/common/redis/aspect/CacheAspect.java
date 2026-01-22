package com.achobeta.themis.common.redis.aspect;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.achobeta.themis.common.redis.annotation.QueryCache;
import com.achobeta.themis.common.redis.annotation.UpdateCache;
import com.achobeta.themis.common.redis.service.IRedisService;
import com.achobeta.themis.common.redis.util.SpelResolverHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RScript;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

import static com.achobeta.themis.common.redis.CacheConstants.*;


/**
 * 更新缓存切面，参考：
 * <a href="https://mp.weixin.qq.com/s/OENxcvRSGtkPtNzjWwK6KQ">转转技术</a>
 *
 * @author Aseubel
 * @date 2025/8/7 上午11:00
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class CacheAspect {

    @Value("${redis.key-prefix:weave-plus:}")
    private String keyPrefix;
    @Value("${redis.time-to-live:86400000}")
    private long ttl;
    private static final long lockTime = 1000;

    private final ThreadPoolTaskExecutor threadPoolExecutor;
    private final SpelResolverHelper spelResolverHelper;
    private final IRedisService redisService;
    private final ObjectMapper objectMapper;

    private static final String SET_SH =
            "local key = KEYS[1]\n"
                    + "local value = ARGV[1]\n"
                    + "local owner = ARGV[2]\n"
                    + "local lockOwner = redis.call('HGET', key, '" + OWNER + "')\n"
                    + "local lockInfo = redis.call('HGET', key, '" + LOCK_INFO + "')\n"
                    + "if lockOwner and lockOwner == owner then\n"
                    + "    redis.call('HMSET', key, '" + LOCK_INFO + "', 'unlocked', '" + VALUE + "', value)\n"
                    + "    redis.call('HDEL', key, '" + UNLOCK_TIME +"')"
                    + "    return 0\n"
                    + "end\n"
                    + "return 1";

    private static final String GET_SH =
            "local key = KEYS[1]\n"
                    + "local newUnlockTime = ARGV[1]\n"
                    + "local owner = ARGV[2]\n"
                    + "local currentTime = tonumber(ARGV[3])\n"
                    + "local value = redis.call('HGET', key, '" + VALUE + "')\n"
                    + "local unlockTime = redis.call('HGET', key, '" + UNLOCK_TIME + "')\n"
                    + "local lockOwner = redis.call('HGET', key, '" + OWNER + "')\n"
                    + "local lockInfo = redis.call('HGET', key, '" + LOCK_INFO + "')\n"
                    + "if unlockTime and currentTime > tonumber(unlockTime or 0) then\n"
                    + "    redis.call('HMSET', key, '" + LOCK_INFO + "', 'locked', '" + UNLOCK_TIME + "', newUnlockTime, '" + OWNER + "', owner)\n"
                    + "    return {value, '" + NEED_QUERY + "'}\n"
                    + "end\n"
                    + "if not value or value == '' then\n"
                    + "    if lockOwner and lockOwner ~= owner then\n"
                    + "        return {value, '" + NEED_WAIT + "'}\n"
                    + "    end\n"
                    + "    redis.call('HMSET', key, '" + LOCK_INFO + "', 'locked', '" + UNLOCK_TIME + "', newUnlockTime, '" + OWNER + "', owner)\n"
                    + "    return {value, '" + NEED_QUERY + "'}\n"
                    + "end\n"
                    + "if lockInfo and lockInfo == 'locked' then \n"
                    + "    return {value, '" + SUCCESS_NEED_QUERY + "'}\n"
                    + "end\n"
                    + "return {value , '" + SUCCESS + "'}";

    private static final String INVALID_SH =
            "local key = KEYS[1]\n"
                    + "local newUnlockTime = tonumber(ARGV[1])\n"
                    + "redis.call('HDEL', key, '" + OWNER + "')\n"
                    + "local value = redis.call('HGET', key, '" + VALUE + "')\n"
                    + "redis.call('HSET', key, '" + LOCK_INFO + "', 'locked')\n"
                    + "if not value or value == '' then\n"
                    + "      return {true, '" + EMPTY_VALUE_SUCCESS + "'}\n"
                    + "end\n"
                    + "if newUnlockTime > 0 then\n"
                    + "      redis.call('HSET', key, '" + UNLOCK_TIME + "', newUnlockTime)\n"
                    + "end\n"
                    + "return {'', '" + SUCCESS + "'}";

//    private static final String SET_SH = loadScript("set.lua");
//    private static final String GET_SH = loadScript("get.lua");
//    private static final String INVALID_SH = loadScript("invalid.lua");

    private static final String SET_SH_SHA = DigestUtil.sha1Hex(SET_SH);
    private static final String GET_SH_SHA = DigestUtil.sha1Hex(GET_SH);
    private static final String INVALID_SH_SHA = DigestUtil.sha1Hex(INVALID_SH);

    @Around("@annotation(updateCache)")
    public Object updateCache(ProceedingJoinPoint joinPoint, UpdateCache updateCache) throws Throwable {

        String key = keyPrefix + spelResolverHelper.resolveSpel(joinPoint, updateCache.key());
        List<Object> results = redisService.execute(INVALID_SH_SHA, INVALID_SH, RScript.ReturnType.MULTI, List.of(key), System.currentTimeMillis() + lockTime);

        String sign = (String) results.get(1);
        switch (sign) {
            case EMPTY_VALUE_SUCCESS:
                Object value = joinPoint.proceed();
                redisService.addToMap(key, VALUE, value);
                return value;
            case SUCCESS:
                return joinPoint.proceed();
        }
        return joinPoint.proceed();
    }

    /**
     * 如果数据为空且锁已过期: 则锁定缓存，返回 NEED_QUERY，同步执行"取数据"并返回结果
     * 如果数据为空且被锁定: 则返回 NEED_WAIT，休眠100ms并再次查询
     * 如果数据不为空且被锁定: 则立即返回SUCCESS_NEED_QUERY和缓存数据，异步执行"取数据"
     * 如果数据不为空且未锁定: 则立即返回SUCCESS和缓存数据
     */
    @Around("@annotation(queryCache)")
    public Object queryCache(ProceedingJoinPoint joinPoint, QueryCache queryCache) throws Throwable {

        String key = keyPrefix + spelResolverHelper.resolveSpel(joinPoint, queryCache.key());
        String newUnlockTime = String.valueOf(System.currentTimeMillis() + lockTime);
        String owner = Thread.currentThread().getName();
        String currentTime = String.valueOf(System.currentTimeMillis());

        List<Object> result = redisService.execute(GET_SH_SHA, GET_SH, RScript.ReturnType.MULTI, List.of(key), newUnlockTime, owner, currentTime);

        String sign = (String) result.get(1);
        long maxWaitTime = System.currentTimeMillis() + 2000; // 最多等待2秒
        while (sign.equals(NEED_WAIT) && System.currentTimeMillis() < maxWaitTime) {
            Thread.sleep(100); // 休眠
            result = redisService.execute(GET_SH_SHA, GET_SH, RScript.ReturnType.MULTI, List.of(key), newUnlockTime, owner, currentTime);
            sign = (String) result.get(1);
        }

        // 从返回结果中获取原始的、未经处理的业务数据字符串
        String valueStr = (String) result.get(0);
        // 提前获取目标方法的返回类型，用于后续的反序列化
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Class<?> returnType = method.getReturnType();

        switch (sign) {
            case NEED_QUERY:
                // 缓存未命中，直接查询源数据并返回
                return queryData(joinPoint, key);
            case SUCCESS_NEED_QUERY:
                // 缓存命中，但数据陈旧，需要异步更新
                threadPoolExecutor.execute(() -> {
                    try {
                        queryData(joinPoint, key);
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                });
                if (valueStr == null) return null;
                return objectMapper.readValue(valueStr, returnType);
            case SUCCESS:
                if (valueStr == null) return null;
                return objectMapper.readValue(valueStr, returnType);
        }
        throw new RuntimeException("未知的缓存状态: " + sign);
    }

    private Object queryData(ProceedingJoinPoint joinPoint, String key) throws Throwable { // 让异常抛出
        try {
            Object value = joinPoint.proceed();
            if (ObjectUtil.isNotEmpty(value)) {
                // 这样确保了存入 Redis 的是标准、可反序列化的 JSON
                String valueAsJson = objectMapper.writeValueAsString(value);
                redisService.execute(SET_SH_SHA, SET_SH, RScript.ReturnType.INTEGER, List.of(key), valueAsJson, Thread.currentThread().getName());
                redisService.setMapExpired(key, ttl);
            }
            return value;
        } catch (Throwable e) {
            // 释放缓存锁
            redisService.execute(INVALID_SH_SHA, INVALID_SH, RScript.ReturnType.MULTI, List.of(key), System.currentTimeMillis() + lockTime);
            log.error("无法更新缓存，从源数据源获取数据失败: {}", key, e);
            throw e;
        }
    }

//    private static String loadScript(String scriptName) {
//        try {
//            // 使用类路径加载Lua脚本文件
//            // 注意生产环境的话路径改为 lua/
//            // 考虑改为通过配置文件指定路径
//            Path path = Paths.get(CacheAspect.class.getClassLoader().getResource("lua\\" + scriptName).toURI());
//            return new String(Files.readAllBytes(path));
//        } catch (IOException | URISyntaxException e) {
//            log.error("Failed to load Lua script: {}", scriptName, e);
//            throw new RuntimeException(e);
//        }
//    }
}
