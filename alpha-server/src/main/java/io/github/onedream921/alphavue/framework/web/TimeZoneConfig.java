package io.github.onedream921.alphavue.framework.web;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;
import java.util.TimeZone;

/**
 * 统一应用 JVM 默认时区，避免无时区 LocalDateTime 使用系统 UTC。
 */
@Configuration
public class TimeZoneConfig {

    private final String timeZone;

    public TimeZoneConfig(@Value("${spring.jackson.time-zone:Asia/Shanghai}") String timeZone) {
        this.timeZone = timeZone;
    }

    @PostConstruct
    public void configureDefaultTimeZone() {
        ZoneId zoneId = ZoneId.of(timeZone);
        TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
    }
}
