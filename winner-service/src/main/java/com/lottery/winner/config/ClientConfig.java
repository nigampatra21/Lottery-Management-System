package com.lottery.winner.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class ClientConfig {

    private RestClient build(String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Bean RestClient lotteryRestClient(@Value("${lottery-service.base-url}") String url) {
        return build(url);
    }

    @Bean RestClient drawRestClient(@Value("${draw-service.base-url}") String url) {
        return build(url);
    }

    @Bean
    RestClient ticketRestClient(@Value("${ticket-service.base-url}") String url)
    {
        return build(url);
    }
}