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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
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
        ZoneId ZONE = ZoneId.of("Europe/Moscow");

        LocalDate today = LocalDate.now(ZONE);
        DayOfWeek dow = today.getDayOfWeek();
        groupService.getStudentList(defaultGroup)
                .thenAccept((List<UserDTO> students) -> {
                    if (students == null || students.isEmpty()) {
                        log.warn("Для группы {} не найдено студентов – уведомление не отправлено", defaultGroup);
                        return;
                    }

                    for (UserDTO student : students) {
                        if (student.getTelegramId() != null) {

                                scheduleService.getForToday(student.getGroupName())
                                        .thenCompose(schedule -> messageSenderService.sendButtonMessage(
                                                    SendMessage.builder()
                                                            .chatId(student.getTelegramId())
                                                            .text(switch (dow) {
                                                                case THURSDAY -> buildMilitarySchedule(schedule);
                                                                case FRIDAY   -> buildHolidaySchedule(schedule);
                                                                case SATURDAY -> buildSaturdaySchedule(schedule);
                                                                default       -> buildSchedule(schedule);
                                                            })
                                                            .replyMarkup(KeyboardUtils.createMarkupWithTomorrow(today))
                                                            .build()
                                            ))
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
        return String.format("Доброе утро!\uD83C\uDF04 Сегодня на повестке дня \uD83D\uDD56: \n\n %s Хорошего дня!☺\uFE0F", schedule);
    }

    private String buildMilitarySchedule(String schedule) {
        return String.format("Военным доброе утро☀\uFE0F☀\uFE0F☀\uFE0F\nОстальным сладких снов!\n%s\n Продуктивного дня!", schedule);
    }

    private String buildHolidaySchedule(String schedule) {
        return String.format("Доброе утро☀\uFE0F☀\uFE0F☀\uFE0F!\n%sПродуктивного дня! \nКста завтра выходные, сегодня по пивку??\uD83D\uDE0F" , schedule);
    }

    private String buildSaturdaySchedule(String schedule) {
        return String.format("Доброе утро☀\uFE0F☀\uFE0F☀\uFE0F\nСегодня фигня, чисто развеяться \n%sПродуктивного дня!\uD83C\uDFD6\uFE0F\nСегодня опять по пивку??\uD83D\uDE0F" , schedule);
    }
}
