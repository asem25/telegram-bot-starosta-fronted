package ru.semavin.bot.service.requests;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.semavin.bot.dto.RequestDTO;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestApiService {

    private final RestClient restClient;
    private final ExecutorService executorService;

    @Value("${api.key}")
    private String apiKey;

    private final String url = "http://localhost:8081/api/v1/";

    public CompletableFuture<List<RequestDTO>> getRequests(String telegramIdStarosta) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<RequestDTO> requests = restClient.get()
                        .uri(url + "/requests?telegramId=" + telegramIdStarosta)
                        .header("API-KEY", apiKey)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {});
                log.info("Получены заявки: {}", requests);
                return requests;
            } catch (Exception e) {
                log.error("Ошибка получения заявок", e);
                throw new RuntimeException(e);
            }
        }, executorService);
    }

    public CompletableFuture<String> sendRequest(String groupName, String telegramTagUser) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                RequestDTO requestDTO = RequestDTO.builder()
                        .groupName(groupName)
                        .telegramTagUser(telegramTagUser)
                        .build();

                log.info("Построенный dto {}", requestDTO);

                String response = restClient.post()
                        .uri(url + "/requests")
                        .header("API-KEY", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestDTO)
                        .retrieve()
                        .body(String.class);

                log.info("Заявка отправлена, ответ от API: {}", response);
                return response;
            } catch (Exception e) {
                log.error("Ошибка отправки заявки", e);
                throw new RuntimeException(e);
            }
        }, executorService);
    }
}
