package io.github.onedream921.alphavue.framework.redis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class RedisKeyTests {

    @Test
    void createsTheRequiredAlphaKeyShape() {
        assertThat(RedisKey.of("system", "cache", "dictionary").value())
                .isEqualTo("alpha:system:cache:dictionary");
    }

    @Test
    void rejectsUncontrolledSegmentsAndNamespaces() {
        assertThatIllegalArgumentException().isThrownBy(() -> RedisKey.of("system", "cache", "a:b"));
        assertThatIllegalArgumentException().isThrownBy(() -> new RedisKey("system:cache:dictionary"));
        assertThatIllegalArgumentException().isThrownBy(() -> new RedisKey("alpha:system:cache"));
        assertThatIllegalArgumentException().isThrownBy(() -> new RedisKey("alpha:system:cache:id:extra"));
    }
}
