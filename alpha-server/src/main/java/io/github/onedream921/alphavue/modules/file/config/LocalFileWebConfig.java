package io.github.onedream921.alphavue.modules.file.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

/**
 * 本地文件访问配置
 */
@Configuration
public class LocalFileWebConfig implements WebMvcConfigurer {

    private final FileStorageProperties properties;

    public LocalFileWebConfig(FileStorageProperties properties) {
        this.properties = properties;
    }

    /**
     * 注册本地文件目录的静态资源映射
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (!properties.isPublicAccess()) {
            return;
        }
        registry.addResourceHandler(properties.localPublicPathPattern())
                .addResourceLocations(properties.localResourceLocation())
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic());
    }
}
