package ru.semavin.bot.botcommands.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.semavin.bot.dto.DeadlineDTO;
import ru.semavin.bot.dto.UserDTO;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.service.deadline.DeadLineCreationService;
import ru.semavin.bot.service.deadline.DeadlineService;
import ru.semavin.bot.service.groups.GroupService;
import ru.semavin.bot.service.users.UserApiService;
import ru.semavin.bot.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
@Slf4j
public class DeadlineCreationStepHandler {
    private final DeadLineCreationService stateService;
    private final MessageSenderService messageSenderService;
    private final GroupService groupService;
    private final DeadlineService deadlineService;

    public void handleStep(Message message) {
        Long chatId = message.getChatId();
        String username = message.getFrom().getUserName();
        String text = message.getText();

        DeadLineCreationService.Step step = stateService.getStep(chatId);
        DeadLineCreationService.Draft draft = stateService.getDraft(chatId);
        log.info("Полученный draft: {}",draft);
        switch (step) {
            case TITLE -> {
                draft.setTitle(text);
                stateService.nextStep(chatId);
                messageSenderService.sendTextMessage(chatId, "Введите описание дедлайна:");
            }
            case DESCRIPTION -> {
                draft.setDescription(text);
                stateService.nextStep(chatId);
                messageSenderService.sendTextMessage(chatId, "Введите дату дедлайна в формате YYYY-MM-DD:");
            }
            case DATE -> {
                try {
                    draft.setDate(LocalDate.parse(text));
                    stateService.nextStep(chatId);

                    groupService.getStudentList(draft.getGroupName())
                            .thenAccept(users -> {
                                List<String> usernames = users.stream()
                                        .map(UserDTO::getUsername)
                                        .collect(Collectors.toList());

                                Map<String, Long> recipientChats = users.stream()
                                        .filter(user -> user.getUsername() != null && user.getTelegramId() != null)
                                        .collect(Collectors.toMap(UserDTO::getUsername, UserDTO::getTelegramId));

                                draft.setRecipients(usernames);
                                log.info("draft перед сохранением: {}", draft);
                                DeadlineDTO dto = draft.toDTO();
                                deadlineService.addDeadline(username, dto, recipientChats)
                                        .thenRun(() -> {
                                            messageSenderService.sendTextMessage(chatId, "✅ Дедлайн создан!");
                                            stateService.clear(chatId);
                                        })
                                        .exceptionally(e -> {
                                            messageSenderService.sendTextMessage(chatId, "❌ Ошибка при создании дедлайна.");
                                            log.error("Ошибка при создании дедлайна: {}", e.getMessage());
                                            return null;
                                        });
                            })
                            .exceptionally(e -> {
                                messageSenderService.sendTextMessage(chatId, "❌ Не удалось получить список студентов группы.");
                                return null;
                            });
                } catch (Exception e) {
                    messageSenderService.sendTextMessage(chatId, "⚠️ Неверный формат даты. Введите в формате YYYY-MM-DD.");
                }
            }
        }
    }
}
