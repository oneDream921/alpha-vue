package io.github.onedream921.alphavue.framework.doc;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.tags.Tag;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Set;

/**
 * OpenAPI 配置
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";
    private static final Set<String> PUBLIC_AUTH_PATHS = Set.of("/api/auth/login", "/api/auth/captcha");

    /**
     * 构建管理端后端接口的 OpenAPI 主文档
     */
    @Bean
    OpenAPI alphaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Alpha Vue 管理端接口")
                        .version("0.0.1")
                        .description("统一返回 ApiResponse；除认证登录和验证码外，接口均使用 Bearer Token 鉴权。")
                        .contact(new Contact().name("Alpha Vue")))
                .components(new Components().addSecuritySchemes(BEARER_AUTH,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("Sa-Token")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }

    /**
     * 为不需要登录的认证接口覆盖根级 Bearer 鉴权要求。
     */
    @Bean
    GlobalOpenApiCustomizer publicAuthenticationCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            PUBLIC_AUTH_PATHS.forEach(path -> {
                var pathItem = openApi.getPaths().get(path);
                if (pathItem != null) {
                    pathItem.readOperations().forEach(operation -> operation.setSecurity(List.of()));
                }
            });
        };
    }

    /**
     * 认证相关接口分组
     */
    @Bean
    GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("auth")
                .pathsToMatch("/api/auth/**")
                .addOpenApiCustomizer(openApi -> openApi.setTags(List.of(
                        tag("认证", "登录、验证码、个人资料与会话接口"))))
                .build();
    }

    /**
     * 系统管理相关接口分组
     */
    @Bean
    GroupedOpenApi systemApi() {
        return GroupedOpenApi.builder()
                .group("system")
                .pathsToMatch("/api/system/**")
                .addOpenApiCustomizer(openApi -> openApi.setTags(List.of(
                        tag("用户管理", "用户 CRUD、角色分配与会话管理"),
                        tag("角色管理", "角色 CRUD 与菜单权限分配"),
                        tag("菜单管理", "菜单 CRUD"),
                        tag("部门管理", "部门 CRUD 与组织树维护"),
                        tag("系统配置", "按受控配置组维护站点、登录、文件与第三方平台设置"))))
                .build();
    }

    /**
     * 文件管理相关接口分组
     */
    @Bean
    GroupedOpenApi fileApi() {
        return GroupedOpenApi.builder()
                .group("file")
                .pathsToMatch("/api/files/**")
                .addOpenApiCustomizer(openApi -> openApi.setTags(List.of(
                        tag("文件管理", "文件上传、列表与删除"))))
                .build();
    }

    /**
     * 审计日志相关接口分组
     */
    @Bean
    GroupedOpenApi logApi() {
        return GroupedOpenApi.builder()
                .group("log")
                .pathsToMatch("/api/logs/**")
                .addOpenApiCustomizer(openApi -> openApi.setTags(List.of(
                        tag("日志查询", "操作日志与登录日志查询"))))
                .build();
    }

    /**
     * 监控运维相关接口分组
     */
    @Bean
    GroupedOpenApi monitorApi() {
        return GroupedOpenApi.builder()
                .group("monitor")
                .pathsToMatch("/api/monitor/**")
                .addOpenApiCustomizer(openApi -> openApi.setTags(List.of(
                        tag("Redis 管理", "Redis 键空间与运行概览"),
                        tag("SQL 日志", "最近 SQL 执行摘要"))))
                .build();
    }

    private static Tag tag(String name, String description) {
        return new Tag().name(name).description(description);
    }
}
