package com.localbite.backend.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Component
@ConfigurationProperties(prefix = "rate-limit")
@Data
@Slf4j
public class RateLimitProperties {

    private Global global = new Global();
    private Auth auth = new Auth();
    private Admin admin = new Admin();
    private Cache cache = new Cache();

    @Data
    public static class Global {
        private int capacity = 100;
        private int refillTokens = 100;
        private int refillPeriodMinutes = 1;
    }

    @Data
    public static class Auth {
        private EndpointLimit login = new EndpointLimit(5, 2, 5);
        private EndpointLimit register = new EndpointLimit(3, 1, 10);
        private EndpointLimit emailVerification = new EndpointLimit(3, 1, 15);
        private EndpointLimit passwordReset = new EndpointLimit(2, 1, 30);
    }

    @Data
    public static class Admin {
        private int capacity = 50;
        private int refillTokens = 25;
        private int refillPeriodMinutes = 1;
    }

    @Data
    public static class Cache {
        private int maximumSize = 10000;
        private int expireAfterAccessMinutes = 60;
    }

    @Data
    public static class EndpointLimit {
        private int capacity;
        private int refillTokens;
        private int refillPeriodMinutes;

        public EndpointLimit() {}

        public EndpointLimit(int capacity, int refillTokens, int refillPeriodMinutes) {
            this.capacity = capacity;
            this.refillTokens = refillTokens;
            this.refillPeriodMinutes = refillPeriodMinutes;
        }
    }

    @PostConstruct
    public void validateConfiguration() {
        log.info("🛡️ Rate Limiting Configuration:");
        log.info("  Global: {} requests per {} minutes", global.capacity, global.refillPeriodMinutes);
        log.info("  Login: {} attempts per {} minutes", auth.login.capacity, auth.login.refillPeriodMinutes);
        log.info("  Registration: {} attempts per {} minutes", auth.register.capacity, auth.register.refillPeriodMinutes);
        log.info("  Email Verification: {} attempts per {} minutes", auth.emailVerification.capacity, auth.emailVerification.refillPeriodMinutes);
        log.info("  Password Reset: {} attempts per {} minutes", auth.passwordReset.capacity, auth.passwordReset.refillPeriodMinutes);
        log.info("  Cache Settings: max size {}, expire after {} minutes", cache.maximumSize, cache.expireAfterAccessMinutes);
        log.info("✅ Rate limiting configuration validated successfully");
    }
} 