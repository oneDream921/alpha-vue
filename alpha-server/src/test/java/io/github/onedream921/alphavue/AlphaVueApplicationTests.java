package io.github.onedream921.alphavue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.servlet.autoconfigure.MultipartProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class AlphaVueApplicationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MultipartProperties multipartProperties;

    @Test
    void contextLoadsWithMigratedDataSource() {
        Integer userTableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES "
                        + "WHERE TABLE_SCHEMA = 'public' AND TABLE_NAME = 'sys_user'",
                Integer.class);

        assertThat(userTableCount).isEqualTo(1);
    }

    @Test
    void multipartLimitsMatchTheDocumentedDefaults() {
        assertThat(multipartProperties.getMaxFileSize().toBytes()).isEqualTo(10 * 1024 * 1024);
        assertThat(multipartProperties.getMaxRequestSize().toBytes()).isEqualTo(12 * 1024 * 1024);
    }
}
