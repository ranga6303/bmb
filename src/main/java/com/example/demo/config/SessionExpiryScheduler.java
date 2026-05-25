package com.example.demo.config;

import com.example.demo.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SessionExpiryScheduler {
    private static final Logger log = LoggerFactory.getLogger(SessionExpiryScheduler.class);

    private final SessionService sessionService;

    public SessionExpiryScheduler(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Scheduled(fixedRate = 60000)
    public void expireSessions() {
        int expired = sessionService.expireActiveSessions();
        if (expired > 0) {
            log.info("Auto-expired {} active sessions", expired);
        }
    }

    @Scheduled(fixedRate = 300000)
    public void autoCancelLockedSessions() {
        int cancelled = sessionService.autoCancelLockedSessions();
        if (cancelled > 0) {
            log.info("Auto-cancelled {} locked sessions (1 hour without approval)", cancelled);
        }
    }
}
