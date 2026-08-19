package com.billim.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 실행 후 http://localhost:8080/swagger-ui.html 에서 확인.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI billimOpenApi() {
        return new OpenAPI().info(new Info()
            .title("빌림 API")
            .description("공공기관에 흩어진 대여·예약 자원을 통합 검색하고 공식 예약처로 연결하는 API")
            .version("v0.1"));
    }
}
