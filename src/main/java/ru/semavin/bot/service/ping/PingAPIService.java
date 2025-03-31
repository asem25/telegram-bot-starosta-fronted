package ru.semavin.bot.service.ping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class PingAPIService {
    private final RestTemplate restTemplate;
    @Value("${api.url}")
    private String apiUrl;
    @Scheduled(fixedRate = 60000 * 3)
    public void pingDailyApi() {
        log.info("Telegram api ping starting");
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(apiUrl + "/", String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Telegram api ping finished success: {}", response.getBody());
            }else{
                log.error("Telegram api ping failed: {}", response.getBody());
            }
        } catch (RestClientException e) {
            log.warn("Telegram api service error");
        }
    }

}
