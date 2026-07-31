package io.github.onedream921.alphavue.framework.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IpLocationServiceTests {
    @Test
    void classifiesPrivateAddressesAndFallsBackWithoutXdb() {
        IpLocationService service = new IpLocationService("");

        assertThat(service.resolve("127.0.0.1")).isEqualTo("内网 IP");
        assertThat(service.resolve("203.0.113.4")).isEqualTo("未知");
    }
}
