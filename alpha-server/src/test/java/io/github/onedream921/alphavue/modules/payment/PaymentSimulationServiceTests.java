package io.github.onedream921.alphavue.modules.payment;

import io.github.onedream921.alphavue.modules.payment.dto.PaymentSimulationRequests;
import io.github.onedream921.alphavue.modules.payment.entity.SysPaymentSimulation;
import io.github.onedream921.alphavue.modules.payment.mapper.SysPaymentSimulationMapper;
import io.github.onedream921.alphavue.modules.payment.service.PaymentSimulationService;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentSimulationServiceTests {
    @Test
    void duplicateCreateReturnsTheExistingOrder() {
        SysPaymentSimulationMapper mapper = mock(SysPaymentSimulationMapper.class);
        SysPaymentSimulation existing = new SysPaymentSimulation();
        existing.setId(8L); existing.setOrderNo("SIMEXISTING"); existing.setChannel("WECHAT"); existing.setAmountFen(100L); existing.setStatus("PENDING");
        when(mapper.selectOne(any())).thenReturn(existing);
        var result = new PaymentSimulationService(mapper).create(new PaymentSimulationRequests.Create("wechat", 100L, "client-key"));
        assertThat(result.id()).isEqualTo(8L);
        verify(mapper, never()).insert(any(SysPaymentSimulation.class));
    }

    @Test
    void completedOrderKeepsItsFirstTerminalState() {
        SysPaymentSimulationMapper mapper = mock(SysPaymentSimulationMapper.class);
        SysPaymentSimulation order = new SysPaymentSimulation();
        order.setId(9L); order.setOrderNo("SIMDONE"); order.setChannel("ALIPAY"); order.setAmountFen(200L); order.setStatus("SUCCEEDED");
        when(mapper.selectById(9L)).thenReturn(order);
        var result = new PaymentSimulationService(mapper).complete(9L, new PaymentSimulationRequests.Complete("failed"));
        assertThat(result.status()).isEqualTo("SUCCEEDED");
        verify(mapper, never()).updateById(any(SysPaymentSimulation.class));
    }
}
