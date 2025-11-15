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
        var stlistFutures = groupService.getStudentList(defaultGroup)
                .exceptionally(ex -> {
                    log.error("Ошибка при получении студентов группы {}", ex.getMessage(), ex);
                    return null;
                });

        var schdListFutures = scheduleService.getForToday(defaultGroup)
                .exceptionally(ex -> {
                    log.error("Ошибка при получении расписания {}", ex.getMessage(), ex);
                    return null;
                });

        var stlist = stlistFutures.join();
        var schdList = schdListFutures.join();

        if (stlist == null || stlist.isEmpty()) {
            log.warn("Для группы {} не найдено студентов – уведомление не отправлено", defaultGroup);
            return;
        }

        if (schdList.contains("занятий нет")) {
            log.info("Для группы {} не найдено пар - увдомление не отправлено", defaultGroup);
            return;
        }


        sendDailyMessage(schdList, stlist);
    }

    private void sendDailyMessage(String schdList, List<UserDTO> stlist) {
        ZoneId ZONE = ZoneId.of("Europe/Moscow");

        LocalDate today = LocalDate.now(ZONE);
        for (UserDTO student : stlist) {
            messageSenderService.sendButtonMessage(buildSendMessage(
                    student.getTelegramId(),
                    schdList,
                    today
            ));
        }
    }

    private SendMessage buildSendMessage(Long telegramId, String schdList, LocalDate today) {
        var dow = today.getDayOfWeek();
        return SendMessage.builder()
                .chatId(telegramId)
                .text(switch (dow) {
                    case THURSDAY -> buildMilitarySchedule(schdList);
                    case FRIDAY -> buildHolidaySchedule(schdList);
                    case SATURDAY -> buildSaturdaySchedule(schdList);
                    default -> buildSchedule(schdList);
                })
                .replyMarkup(KeyboardUtils.createMarkupWithTomorrow(today))
                .build();
    }

    private String buildSchedule(String schedule) {
        return String.format("Доброе утро!\uD83C\uDF04" +
                " Сегодня на повестке дня \uD83D\uDD56: \n\n %s Хорошего дня!☺\uFE0F", schedule);
    }

    private String buildMilitarySchedule(String schedule) {
        return String.format("Военным доброе утро☀️☀️☀️\n" +
                "Остальным сладких снов!\n%s\n Продуктивного дня!", schedule);
    }

    private String buildHolidaySchedule(String schedule) {
        return String.format("Доброе утро☀️☀️☀️!\n" +
                "%sПродуктивного дня! \nКста завтра выходные, сегодня по пивку??\uD83D\uDE0F", schedule);
    }

    private String buildSaturdaySchedule(String schedule) {
        return String.format("Доброе утро☀️☀️☀️\n" +
                "Сегодня фигня, чисто развеяться \n%sПродуктивного дня!" +
                "\uD83C\uDFD6\uFE0F\nСегодня опять по пивку??\uD83D\uDE0F", schedule);
    }
}
