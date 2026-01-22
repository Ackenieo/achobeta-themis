package com.achobeta.themis.config;

import com.achobeta.themis.interceptor.LogInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author Aseubel
 * @date 2025/8/5 上午9:31
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 所有的请求都加上 Log 拦截器
        registry.addInterceptor(new LogInterceptor());
    }

}
