package io.github.onedream921.alphavue.modules.payment.vo;

import io.github.onedream921.alphavue.modules.payment.entity.SysPaymentSimulation;
import java.time.LocalDateTime;

public record PaymentSimulationVo(long id, String orderNo, String channel, long amountFen, String status, LocalDateTime createdAt) {
    public static PaymentSimulationVo from(SysPaymentSimulation value) { return new PaymentSimulationVo(value.getId(), value.getOrderNo(), value.getChannel(), value.getAmountFen(), value.getStatus(), value.getCreatedAt()); }
}
