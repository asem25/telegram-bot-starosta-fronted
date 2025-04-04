package ru.semavin.bot.service.deadline;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.semavin.bot.dto.DeadlineDTO;
import ru.semavin.bot.util.KeyboardUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class DeadLineMessageUtil {
    public static List<SendMessage> buildDeadlineMessagesWithButtons(String groupName,
                                                              Long chatId,
                                                              List<DeadlineDTO> deadlines,
                                                              boolean isStarosta) {
        if (deadlines.isEmpty()) {
            return List.of(SendMessage.builder()
                    .chatId(chatId.toString())
                    .text("\uD83D\uDCCD В группе *" + groupName + "* пока нет назначенных дедлайнов.")
                    .parseMode("Markdown")
                    .build());
        }

        Map<LocalDate, List<DeadlineDTO>> groupedByDate = deadlines.stream()
                .sorted(Comparator.comparing(DeadlineDTO::getDueDate))
                .collect(Collectors.groupingBy(DeadlineDTO::getDueDate, LinkedHashMap::new, Collectors.toList()));

        LocalDate today = LocalDate.now();
        List<SendMessage> messages = new ArrayList<>();

        for (Map.Entry<LocalDate, List<DeadlineDTO>> entry : groupedByDate.entrySet()) {
            LocalDate date = entry.getKey();
            long daysLeft = today.until(date).getDays();
            String urgencyEmoji = getUrgencyEmoji(daysLeft);

            for (DeadlineDTO d : entry.getValue()) {
                String text = "\uD83D\uDCC5 *" + date + "* " + urgencyEmoji + "\n" +
                        "— \uD83D\uDCCC *" + d.getTitle() + "*\n" +
                        "  \uD83D\uDCDD " + d.getDescription() + "\n";

                SendMessage msg = SendMessage.builder()
                        .chatId(chatId.toString())
                        .text(text)
                        .parseMode("Markdown")
                        .replyMarkup(isStarosta ? KeyboardUtils.buildEditDeleteButtons(d.getUuid()) : null)
                        .build();

                messages.add(msg);
            }
        }

        return messages;
    }

    private static String getUrgencyEmoji(long daysLeft) {
        if (daysLeft < 0) return "❗️";       // просрочено
        if (daysLeft == 0) return "🔴";       // сегодня
        if (daysLeft <= 3) return "🟠";       // срочно
        if (daysLeft <= 5) return "🟡";       // скоро
        return "🟢";                          // есть время
    }
}
