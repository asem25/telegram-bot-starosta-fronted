package ru.semavin.bot.service.groups;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import org.springframework.web.client.RestClient;
import ru.semavin.bot.dto.GroupDTO;
import ru.semavin.bot.dto.UserDTO;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupApiService {

    private final RestClient restClient;
    private final ExecutorService executorService;

    @Value("${api.key}")
    private String apiKey;

    @Value("${api.url}")
    private String url;

    public CompletableFuture<GroupDTO> setStarosta(String groupName, String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Назначение старосты: группа {}, пользователь {}", groupName, username);
                return restClient.patch()
                        .uri(url + "/groups/setStarosta?groupName={group}&starostaUsername={user}", groupName, username)
                        .header("API-KEY", apiKey)
                        .retrieve()
                        .body(GroupDTO.class);
            } catch (Exception e) {
                log.error("Ошибка при назначении старосты", e);
                throw new RuntimeException(e);
            }
        }, executorService);
    }

    public CompletableFuture<GroupDTO> deleteStarosta(String groupName, String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Удаление старосты: группа {}, пользователь {}", groupName, username);
                return restClient.patch()
                        .uri(url + "/groups/delStarosta?groupName={group}&starostaUsername={user}", groupName, username)
                        .header("API-KEY", apiKey)
                        .retrieve()
                        .body(GroupDTO.class);
            } catch (Exception e) {
                log.error("Ошибка при удалении старосты", e);
                throw new RuntimeException(e);
            }
        }, executorService);
    }
    public CompletableFuture<GroupDTO> getGroup(String groupName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Получение группы: {}", groupName);
                return restClient.get()
                        .uri(url + "/groups?groupName={group}", groupName)
                        .header("API-KEY", apiKey)
                        .retrieve()
                        .body(GroupDTO.class);
            } catch (Exception e) {
                log.error("Ошибка при удалении старосты", e);
                throw new RuntimeException(e);
            }
        }, executorService);
    }

    public CompletableFuture<Optional<UserDTO>> getStarosta(String groupName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Получение старосты группы: {}", groupName);
                UserDTO dto = restClient.get()
                        .uri(url + "/groups/getStarosta?groupName={group}", groupName)
                        .header("API-KEY", apiKey)
                        .retrieve()
                        .body(UserDTO.class);
                if (dto == null) {
                    return Optional.empty();
                }
                return Optional.of(dto);
            } catch (HttpClientErrorException.NotFound e) {
                log.info("Староста группы {} не найден (404)", groupName);
                return Optional.empty();
            } catch (Exception e) {
                log.error("Ошибка при получении старосты группы {}", groupName, e);
                throw new RuntimeException("Ошибка при запросе старосты", e);
            }
        }, executorService);
    }
}
