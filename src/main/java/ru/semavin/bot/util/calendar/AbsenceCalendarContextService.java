package ru.semavin.bot.util.calendar;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.semavin.bot.dto.SkipNotificationDTO;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.service.notification.SkipNotificationService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AbsenceCalendarContextService {

    private final ConcurrentHashMap<Long, SkipNotificationDTO> draftByUser = new ConcurrentHashMap<>();
    private final SkipNotificationService skipNotificationService;
    private final MessageSenderService messageSenderService;

    public void saveDates(Long userId, LocalDate from, LocalDate to) {
        draftByUser.putIfAbsent(userId, new SkipNotificationDTO());
        SkipNotificationDTO dto = draftByUser.get(userId);
        dto.setFromDate(from);
        dto.setToDate(to);
    }

    public void saveDescription(Long userId, String description) {
        SkipNotificationDTO dto = draftByUser.get(userId);
        if (dto != null) {
            dto.setDescription(description);
            skipNotificationService.notifyStarosta(dto);
            messageSenderService.sendTextMessage(userId, "✅ Староста уведомлен!");
            clear(userId);
        }

    }
    public void setUserContext(Long userId, String username, String groupName) {
        draftByUser.putIfAbsent(userId, new SkipNotificationDTO());
        SkipNotificationDTO dto = draftByUser.get(userId);
        dto.setUsername(username);
        dto.setGroupName(groupName);
    }

    public Optional<SkipNotificationDTO> getDraft(Long userId) {
        return Optional.ofNullable(draftByUser.get(userId));
    }
    public void setDraft(Long userId, SkipNotificationDTO dto){
        draftByUser.put(userId, dto);
    }
    public void clear(Long userId) {
        draftByUser.remove(userId);
    }

    public boolean isReadyToConfirm(Long userId) {
        SkipNotificationDTO dto = draftByUser.get(userId);
        return dto != null && dto.getFromDate() != null && dto.getToDate() != null;
    }

    public boolean isAwaitingDescription(Long userId) {
        SkipNotificationDTO dto = draftByUser.get(userId);
        return dto != null && dto.getFromDate() != null && dto.getToDate() != null && dto.getDescription() == null;
    }
}
