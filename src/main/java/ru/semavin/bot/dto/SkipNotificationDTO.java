package ru.semavin.bot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkipNotificationDTO {
    private String username;
    private String groupName;
    private String description;
    private LocalDate fromDate;
    private LocalDate toDate;

    public boolean isExpired() {
        return LocalDate.now().isAfter(toDate);
    }
}
