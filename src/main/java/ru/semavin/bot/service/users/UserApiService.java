package ru.semavin.bot.service.users;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import ru.semavin.bot.dto.UserDTO;
import ru.semavin.bot.util.exceptions.BadRequestException;
import ru.semavin.bot.util.exceptions.EntityNotFoundException;


import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserApiService {

    private final RestClient restClient;
    private final ExecutorService executorService;
    private final ObjectMapper objectMapper;

    @Value("${api.key}")
    private String apiKey;

    @Value("${api.url}")
    private String url;

    public CompletableFuture<String> registerUser(UserDTO dto) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Запрос регистрации пользователя с id: {}", dto.getTelegramId());
                return restClient.post()
                        .uri(url + "/users")
                        .header("API-KEY", apiKey)
                        .body(dto)
                        .retrieve()
                        .body(String.class);
            } catch (HttpClientErrorException.NotFound notFound) {
                log.error("Ошибка при регистрации пользователя: {}", notFound.getMessage());
                String responseBody = notFound.getResponseBodyAsString();
                try {
                    JsonNode root = objectMapper.readTree(responseBody);
                    String errorDescription = root.path("error_description").asText();
                    log.info("Error description: {}", errorDescription);
                    throw new EntityNotFoundException(errorDescription);
                }  catch (IOException exec) {
                        log.error("Ошибка парсинга ответа: {}", exec.getMessage());
                    }

                    return null;
                }
                catch (HttpClientErrorException.BadRequest badRequest) {
                log.error("Ошибка при регистрации пользователя: {}", badRequest.getMessage());
                String Body = badRequest.getResponseBodyAsString();
                try {
                    JsonNode root = objectMapper.readTree(Body);
                    String errorDescription = root.path("error_description").asText();
                    log.info("Error description: {}", errorDescription);
                    throw new BadRequestException(errorDescription);
                }
                catch (Exception laste) {
                    log.error("Ошибка при получении пользователя: ", laste);
                    throw new RuntimeException(laste);
                }
            }
        }, executorService);

    }

    @Cacheable(value = "users", key = "#id")
    public CompletableFuture<UserDTO> getUser(String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Запрос пользователя с tag: {}", id);
                return restClient.get()
                        .uri(url + "/users?telegramTag={id}", id)
                        .header("API-KEY", apiKey)
                        .retrieve()
                        .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                            if (response.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                                throw new EntityNotFoundException("Пользователь не найден");
                            }
                        })
                        .body(UserDTO.class);
            } catch (HttpClientErrorException.NotFound e) {
                log.error("Ошибка при получение пользователя: {}", e.getMessage());
                String responseBody = e.getResponseBodyAsString();
                try {
                    JsonNode root = objectMapper.readTree(responseBody);
                    String errorDescription = root.path("error_description").asText();
                    log.info("Error description: {}", errorDescription);
                    throw new EntityNotFoundException(errorDescription);
                } catch (IOException ex) {
                    log.error("Ошибка парсинга ответа: {}", ex.getMessage());
                }

                return null;
            } catch (Exception e) {
                log.error("Ошибка при получении пользователя с id {}: ", id, e);
                throw new RuntimeException(e);
            }
        }, executorService);
    }

    public CompletableFuture<String> updateUser(UserDTO userDTO) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return restClient.patch()
                        .uri(url + "/users")
                        .header("API-KEY", apiKey)
                        .body(userDTO)
                        .retrieve()
                        .body(String.class);
            } catch (HttpClientErrorException e) {
                log.error("Ошибка при обновлении пользователя: ", e);
                if (e instanceof HttpClientErrorException.NotFound notFoundException){
                    log.error("Ошибка при получение пользователя: {}", e.getMessage());
                    String responseBody = notFoundException.getResponseBodyAsString();
                    try {
                        JsonNode root = objectMapper.readTree(responseBody);
                        String errorDescription = root.path("error_description").asText();
                        log.info("Error description: {}", errorDescription);
                        throw new EntityNotFoundException(errorDescription);
                    } catch (IOException ex) {
                        log.error("Ошибка парсинга ответа: {}", ex.getMessage());
                    }
                }

                return null;
            }
        }, executorService);
    }

}