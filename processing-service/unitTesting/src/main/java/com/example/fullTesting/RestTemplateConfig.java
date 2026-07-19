package com.example.fullTesting;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // Đợi tối đa 20 giây để thiết lập kết nối (Connection)
        factory.setConnectTimeout(20000);

        // Đợi tối đa 20 giây để nhận được dữ liệu (Read)
        factory.setReadTimeout(20000);

        return new RestTemplate(factory);
    }
}