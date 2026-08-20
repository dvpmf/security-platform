package com.rongan.security_system.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
               .allowedOriginPatterns("*")  // 允许所有来源，生产环境可指定具体域名
               .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
               .allowCredentials(true)      // 允许携带凭证 例如 Cookie
               .maxAge(3600);
    }
}
