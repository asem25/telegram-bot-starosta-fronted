package ru.semavin.bot.service.deadline;

import org.springframework.stereotype.Service;
import ru.semavin.bot.dto.DeadlineDTO;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DeadLineCreationService {
    public enum Step {
        TITLE,
        DESCRIPTION,
        DATE,
        RECIPIENTS,
        COMPLETE
    }

    public static class Draft {
        private String username;
        private String groupName;
        private String title;
        private String description;
        private LocalDate date;
        private List<String> recipients = new ArrayList<>();

        public DeadlineDTO toDTO() {
            return DeadlineDTO.builder()
                    .title(title)
                    .description(description)
                    .dueDate(date)
                    .groupName(groupName)
                    .recipients(recipients)
                    .build();
        }
    }

    private final Map<Long, Step> currentStep = new ConcurrentHashMap<>();
    private final Map<Long, Draft> userDrafts = new ConcurrentHashMap<>();

    public void start(Long chatId, String username) {
        Draft draft = new Draft();
        draft.username = username;
        userDrafts.put(chatId, draft);
        currentStep.put(chatId, Step.TITLE);
    }

    public Step getStep(Long chatId) {
        return currentStep.getOrDefault(chatId, Step.COMPLETE);
    }

    public Draft getDraft(Long chatId) {
        return userDrafts.get(chatId);
    }

    public void nextStep(Long chatId) {
        Step step = getStep(chatId);
        switch (step) {
            case TITLE -> currentStep.put(chatId, Step.DESCRIPTION);
            case DESCRIPTION -> currentStep.put(chatId, Step.DATE);
            case DATE -> currentStep.put(chatId, Step.RECIPIENTS);
            case RECIPIENTS -> currentStep.put(chatId, Step.COMPLETE);
            default -> {}
        }
    }

    public void clear(Long chatId) {
        currentStep.remove(chatId);
        userDrafts.remove(chatId);
    }

}
