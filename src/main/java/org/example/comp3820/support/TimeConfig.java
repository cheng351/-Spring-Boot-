package org.example.comp3820.support;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Supplies the clock the risk rules read "today" from.
 *
 * <p>It exists so that no class further down calls {@code LocalDate.now()} directly. Tests
 * substitute a fixed clock, which is what makes assertions such as "this reading is from last
 * night" deterministic instead of failing once a day at midnight.
 */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
