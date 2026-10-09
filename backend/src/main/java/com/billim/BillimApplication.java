package com.billim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
// Page 객체를 그대로 JSON으로 내보내면 형식이 버전마다 달라질 수 있어, 고정된 DTO 형식(PagedModel)으로 응답한다.
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
public class BillimApplication {
    public static void main(String[] args) {
        SpringApplication.run(BillimApplication.class, args);
    }
}