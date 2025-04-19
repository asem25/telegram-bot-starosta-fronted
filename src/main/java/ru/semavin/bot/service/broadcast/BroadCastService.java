package ru.semavin.bot.service.broadcast;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.util.RateLimiter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.semavin.bot.dto.UserDTO;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.service.groups.GroupService;
import ru.semavin.bot.service.users.UserService;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Делает фактическую рассылку сообщений группе.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BroadCastService {

    private final GroupService groupService;
    private final MessageSenderService sender;

    /** Ключ = broadcastUuid:telegramId */
    private final Set<String> sent = new ConcurrentSkipListSet<>();
    private final ExecutorService singleThread = Executors.newSingleThreadExecutor();
    /** Очистка каждые 6 ч, чтобы set не рос бесконечно */
    @Scheduled(fixedDelayString = "${broadcast.cleanup-ms:21600000}")
    public void cleanup() {
        sent.clear();
    }

    /**
     * Рассылает сообщение группе, гарантируя:
     *  * один раз на каждого студента в рамках данного broadcast
     *  * не более 20 сообщений / секунду
     */
    public CompletableFuture<Void> broadcastToGroup(String group, String text, String author) {

        UUID bid = UUID.randomUUID();
        return groupService.getStudentList(group).thenCompose(users -> {

            List<CompletableFuture<String>> tasks = users.stream()
                    .map(UserDTO::getTelegramId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .map(id -> CompletableFuture.supplyAsync(() -> {
                        String key = bid + ":" + id;
                        if (!sent.add(key)) return "dup";
                        return sender.sendMessageWithMarkDown(id, String.format("❗Оповещение от старосты❗\n%s", text)).join();
                    }, singleThread))
                    .toList();

            return CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]));
        });
    }

    /** Отвечает «нет доступа» тем, кто не староста */
    public CompletableFuture<Void> replyForbidden(Long chatId) {
        return sender.sendMessageWithMarkDown(chatId, "⛔ У вас нет доступа к этой команде!")
                .thenAccept(v -> {});
    }
}
