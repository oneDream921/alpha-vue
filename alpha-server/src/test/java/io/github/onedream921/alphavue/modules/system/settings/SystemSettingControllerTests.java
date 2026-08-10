package io.github.onedream921.alphavue.modules.system.settings;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SystemSettingControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesOnlyTheSafePublicBootstrapSettingsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/system/settings/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.site").isMap())
                .andExpect(jsonPath("$.data.login.captchaEnabled").exists())
                .andExpect(jsonPath("$.data.login.rememberMeEnabled").exists());
    }
}
