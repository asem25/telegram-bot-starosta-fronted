package ru.semavin.bot.service.ping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
@RequiredArgsConstructor
public class PingAPIService {
    private final RestClient restClient;
    @Value("${api.url}")
    private String apiUrl;
    @Scheduled(fixedRate = 60000 * 3)
    public void ping() {
        log.info("Pinging API" );
        HttpStatusCode response = restClient.get().uri(apiUrl + "/").retrieve().toBodilessEntity().getStatusCode();
        if (response.is2xxSuccessful()) {
            log.info("Pinging API OK");
        }else {
            log.info("Pinging API ERROR");
        }
    }

}
