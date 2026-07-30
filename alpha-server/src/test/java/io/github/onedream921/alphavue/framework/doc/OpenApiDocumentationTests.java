package io.github.onedream921.alphavue.framework.doc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "springdoc.api-docs.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocumentationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesOnlyAuthenticationTagsInAuthenticationGroup() throws Exception {
        mockMvc.perform(get("/v3/api-docs/auth"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags[?(@.name == '认证')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == '文件管理')]").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/system/configs']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/auth/test-token']").doesNotExist());
    }

    @Test
    void exposesConfigurationTagInSystemGroup() throws Exception {
        mockMvc.perform(get("/v3/api-docs/system"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags[?(@.name == '参数配置')]").exists())
                .andExpect(jsonPath("$.paths['/api/system/configs']").exists());
    }

    @Test
    void usesJavadocSummariesAndBearerOverridesForPublicAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs/auth"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.summary").value("使用账号密码登录并返回 Bearer Token"))
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.security").isArray())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.security").isEmpty())
                .andExpect(jsonPath("$.security[0].bearerAuth").isArray());
    }

    @Test
    void keepsPrivateDownloadHiddenAndDocumentsPaginationAndUnifiedResponse() throws Exception {
        mockMvc.perform(get("/v3/api-docs/file"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/files/{id}/content']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/files/upload'].post.requestBody.content['multipart/form-data'].schema").exists())
                .andExpect(jsonPath("$.paths['/api/files'].get.responses.200.content['*/*'].schema").exists())
                .andExpect(jsonPath("$.components.schemas.PageResponseFileView").exists())
                .andExpect(jsonPath("$.components.schemas.ApiResponsePageResponseFileView.properties.code").exists())
                .andExpect(jsonPath("$.components.schemas.ApiResponsePageResponseFileView.properties.traceId").exists());
    }
}
