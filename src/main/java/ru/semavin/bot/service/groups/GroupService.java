package ru.semavin.bot.service.groups;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.semavin.bot.dto.GroupDTO;
import ru.semavin.bot.dto.UserDTO;
import ru.semavin.bot.service.users.UserApiService;
import ru.semavin.bot.service.users.UserService;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class GroupService {
    private final GroupApiService groupApiService;
    private final UserService userService;
    public CompletableFuture<GroupDTO> setStarosta(String groupName, String username) {
        return groupApiService.setStarosta(groupName, username);
    }

    public CompletableFuture<GroupDTO> deleteStarosta(String groupName, String username) {
        return groupApiService.deleteStarosta(groupName, username);
    }

    public CompletableFuture<Optional<UserDTO>> getStarosta(String groupName) {
        return groupApiService.getStarosta(groupName);
    }
    public CompletableFuture<List<UserDTO>> getStudentList(String groupName) {
        return groupApiService.getGroup(groupName)
                .thenCompose(group -> {
                    List<String> usernames = group.getNames_students();
                    List<CompletableFuture<UserDTO>> futures = usernames.stream()
                            .map(userService::getUserForTelegramTag)
                            .toList();

                    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                            .thenApply(v -> futures.stream()
                                    .map(CompletableFuture::join)
                                    .toList());
                });
    }
}
