package io.github.onedream921.alphavue.modules.log;

import io.github.onedream921.alphavue.framework.web.IpLocationService;
import io.github.onedream921.alphavue.modules.log.entity.SysOperLog;
import io.github.onedream921.alphavue.modules.log.mapper.SysLoginLogMapper;
import io.github.onedream921.alphavue.modules.log.mapper.SysOperLogMapper;
import io.github.onedream921.alphavue.modules.log.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditLogServiceTests {
    @Test
    void persistsClientAndDeviceMetadataOnOperationLog() {
        SysLoginLogMapper loginMapper = mock(SysLoginLogMapper.class);
        SysOperLogMapper operMapper = mock(SysOperLogMapper.class);
        IpLocationService locationService = mock(IpLocationService.class);
        when(locationService.resolve("127.0.0.1")).thenReturn("内网 IP");

        new AuditLogService(loginMapper, operMapper, locationService).recordOperation(
                1L, "admin", "System", "Create department", BusinessType.CREATE,
                "POST", "/api/system/depts", 200, true, "127.0.0.1", 12L, "trace",
                null, null, "Mozilla", "pc-admin", "desktop-1", "Office", null, null);

        ArgumentCaptor<SysOperLog> captor = ArgumentCaptor.forClass(SysOperLog.class);
        verify(operMapper).insert(captor.capture());
        assertThat(captor.getValue().getClientId()).isEqualTo("pc-admin");
        assertThat(captor.getValue().getDeviceId()).isEqualTo("desktop-1");
    }
}
