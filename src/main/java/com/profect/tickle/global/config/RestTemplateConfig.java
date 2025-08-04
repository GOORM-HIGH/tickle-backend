package com.profect.tickle.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Configuration
public class RestTemplateConfig {

    @Bean(name = "kopisRestTemplate")
    public RestTemplate kopisRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        StringHttpMessageConverter converter = new StringHttpMessageConverter(Charset.forName("UTF-8"));
        restTemplate.setMessageConverters(Collections.singletonList(converter));

        return restTemplate;
    }

}
