package com.example.reactplatform.global.config;

import com.example.reactplatform.global.log.RequestTraceInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC Configuration
 * Configures view controllers and other web settings
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RequestTraceInterceptor apiLoggingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiLoggingInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/actuator/**",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/webjars/**",
                        "/vendor/**",
                        "/favicon.ico",
                        "/swagger-ui/**",
                        "/v3/api-docs/**");
    }

    /**
     * Register view controllers for simple page mappings
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 홈 페이지
        registry.addViewController("/").setViewName("redirect:/home");
    }

    /**
     * Configure resource handlers for uploaded files
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + System.getProperty("user.dir") + "/uploads/");
    }
}
