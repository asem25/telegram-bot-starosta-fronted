package ru.semavin.bot.service.schedules;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.semavin.bot.dto.UserDTO;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.service.groups.GroupService;
import ru.semavin.bot.util.KeyboardUtils;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EveryDayScheduleCronService {
    private final ScheduleService scheduleService;
    private final GroupService groupService;
    private final MessageSenderService messageSenderService;
    private final String defaultGroup = "М3О-403С-22";

    @Scheduled(cron = "0 0 7 * * MON-SAT", zone = "Europe/Moscow")
    public void sendScheduleForToday() {
        groupService.getStudentList(defaultGroup)
                .thenAccept((List<UserDTO> students) -> {
                    if (students == null || students.isEmpty()) {
                        log.warn("Для группы {} не найдено студентов – уведомление не отправлено", defaultGroup);
                        return;
                    }

                    for (UserDTO student : students) {
                        if (student.getTelegramId() != null) {
                                    scheduleService.getForToday(student.getGroupName())
                                    .thenCompose(schedule -> {
                                        return messageSenderService.sendButtonMessage(
                                            SendMessage.builder()
                                                    .chatId(student.getTelegramId())
                                                    .text(buildSchedule(schedule))
                                                    .replyMarkup(KeyboardUtils.createMarkupWithTomorrow(LocalDate.now()))
                                                    .build());

                                    })
                                    .exceptionally(e -> {
                                        log.error("Ошибка при получении расписания на сегодня: {}", e.getMessage());
                                        return null;
                                    })
                                    .thenApply(messageId -> {
                                        log.debug("Команда 'Сегодня' выполнена для пользователя {}", student.getTelegramId());
                                        return null;
                                    });
                        }
                    }
                    log.debug("Расписание отправлено студентам группы {}", defaultGroup);
                })
                .exceptionally(ex -> {
                    log.error("Ошибка при оповещении студентов: {}", ex.getMessage());
                    return null;
                });
    }

    private String buildSchedule(String schedule) {
        return String.format("Доброе утро!\uD83C\uDF04 Сегодня в повестке дня \uD83D\uDD56: \n\n %s Хорошего дня!☺\uFE0F", schedule);
    }
}
