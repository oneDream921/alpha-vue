package io.github.onedream921.alphavue.framework.doc;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.tags.Tag;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 配置
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

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
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .addTagsItem(new Tag().name("认证").description("登录、验证码、个人资料与会话接口"))
                .addTagsItem(new Tag().name("用户管理").description("用户 CRUD、角色分配与会话管理"))
                .addTagsItem(new Tag().name("角色管理").description("角色 CRUD 与菜单权限分配"))
                .addTagsItem(new Tag().name("菜单管理").description("菜单 CRUD"))
                .addTagsItem(new Tag().name("部门管理").description("部门 CRUD 与组织树维护"))
                .addTagsItem(new Tag().name("文件管理").description("文件上传、列表与删除"))
                .addTagsItem(new Tag().name("日志查询").description("操作日志与登录日志查询"));
    }

    /**
     * 认证相关接口分组
     */
    @Bean
    GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("auth")
                .pathsToMatch("/api/auth/**")
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
                .build();
    }
}
