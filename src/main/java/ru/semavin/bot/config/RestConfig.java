package ru.semavin.bot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    @Bean
    public RestClient restClient(){
        return RestClient.builder().build();
    }
    @Bean
    public ObjectMapper objectMapper(){return new ObjectMapper();}


}
