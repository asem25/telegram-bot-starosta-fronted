package ru.semavin.bot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.semavin.bot.dto.GroupDTO;
import ru.semavin.bot.dto.UserDTO;
import ru.semavin.bot.service.groups.GroupApiService;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
@Service
@Slf4j
@RequiredArgsConstructor
public class GroupService {
    private final GroupApiService groupApiService;

    public CompletableFuture<GroupDTO> setStarosta(String groupName, String username) {
        return groupApiService.setStarosta(groupName, username);
    }

    public CompletableFuture<GroupDTO> deleteStarosta(String groupName, String username) {
        return groupApiService.deleteStarosta(groupName, username);
    }

    public CompletableFuture<Optional<UserDTO>> getStarosta(String groupName) {
        return groupApiService.getStarosta(groupName);
    }

}
