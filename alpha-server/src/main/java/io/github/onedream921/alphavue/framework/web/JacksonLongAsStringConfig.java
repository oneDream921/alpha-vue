package io.github.onedream921.alphavue.framework.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * JSON 序列化配置
 *
 * 防止雪花 Long 主键在 JavaScript 中发生精度丢失
 */
@Configuration
public class JacksonLongAsStringConfig {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 将 Long 包装类型按字符串输出，保留浏览器可精确传回的主键值
     */
    @Bean
    SimpleModule longAsStringModule() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, new ValueSerializer<>() {
            @Override
            public void serialize(Long value, JsonGenerator generator, SerializationContext context) {
                generator.writeString(value.toString());
            }
        });
        module.addSerializer(LocalDateTime.class, new ValueSerializer<>() {
            @Override
            public void serialize(LocalDateTime value, JsonGenerator generator, SerializationContext context) {
                generator.writeString(DATE_TIME_FORMATTER.format(value));
            }
        });
        return module;
    }
}
