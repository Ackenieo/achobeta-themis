package com.achobeta.themis.interceptor;
/**
 * <p>
 * 描述：具体登录检查切面类
 * </p>
 *
 * @Author: ZGjie20
 * @version: 1.0.0
 */
import com.achobeta.themis.common.annotation.LoginRequired;
import com.achobeta.themis.common.exception.AuthenticationException;
import com.achobeta.themis.common.exception.UnauthorizedException;
import com.achobeta.themis.common.util.JwtUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class LoginCheckAspect {


    @Resource
    private JwtUtil jwtUtil;

    @Around("@annotation(com.achobeta.themis.common.annotation.LoginRequired) || @within(com.achobeta.themis.common.annotation.LoginRequired)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attrs != null ? attrs.getRequest() : null;
        if (request == null) {
            throw new AuthenticationException("无法获取请求对象，禁止访问！");
        }
        String accessToken = request.getHeader("Authorization");
        if (accessToken != null && accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);
        }
        boolean accessTokenValid = accessToken != null && jwtUtil.validateToken(accessToken) && jwtUtil.isAccessToken(accessToken);
        if (accessTokenValid) {
            // 保存用户信息到Spring Security上下文
            jwtUtil.saveUserInfoToSecurityContext(accessToken);
          //  ThreadLocal
            return joinPoint.proceed();
        }
        else {
            throw new AuthenticationException("未登录或token无效，禁止访问！");
        }

    }
}

