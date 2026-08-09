package io.github.onedream921.alphavue.modules.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.github.onedream921.alphavue.modules.system.entity.SystemEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_payment_simulation")
public class SysPaymentSimulation extends SystemEntity {
    private String orderNo;
    private String channel;
    private Long amountFen;
    private String status;
    private String idempotencyKey;
}
