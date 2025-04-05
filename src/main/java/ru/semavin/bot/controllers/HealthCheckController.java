package ru.semavin.bot.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
@Slf4j
public class HealthCheckController {
    @GetMapping
    public ResponseEntity<String> healthCheck() {
        log.info("Пришел запрос от API");
        return ResponseEntity.ok("HealthCheck is ok");
    }
}
