package io.github.onedream921.alphavue.modules.payment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.onedream921.alphavue.common.exception.BusinessException;
import io.github.onedream921.alphavue.common.exception.PublicErrorMessage;
import io.github.onedream921.alphavue.modules.payment.dto.PaymentSimulationRequests;
import io.github.onedream921.alphavue.modules.payment.entity.SysPaymentSimulation;
import io.github.onedream921.alphavue.modules.payment.mapper.SysPaymentSimulationMapper;
import io.github.onedream921.alphavue.modules.payment.vo.PaymentSimulationVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;
import java.util.UUID;

@Service
public class PaymentSimulationService {
    private final SysPaymentSimulationMapper mapper;
    public PaymentSimulationService(SysPaymentSimulationMapper mapper) { this.mapper = mapper; }
    @Transactional
    public PaymentSimulationVo create(PaymentSimulationRequests.Create request) {
        String channel = request.channel().toUpperCase(Locale.ROOT);
        if (!channel.equals("WECHAT") && !channel.equals("ALIPAY")) throw invalid();
        SysPaymentSimulation existing = mapper.selectOne(new LambdaQueryWrapper<SysPaymentSimulation>().eq(SysPaymentSimulation::getIdempotencyKey, request.idempotencyKey()));
        if (existing != null) return PaymentSimulationVo.from(existing);
        SysPaymentSimulation order = new SysPaymentSimulation();
        order.setOrderNo("SIM" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase(Locale.ROOT));
        order.setChannel(channel); order.setAmountFen(request.amountFen()); order.setStatus("PENDING"); order.setIdempotencyKey(request.idempotencyKey());
        mapper.insert(order); return PaymentSimulationVo.from(order);
    }
    @Transactional
    public PaymentSimulationVo complete(long id, PaymentSimulationRequests.Complete request) {
        SysPaymentSimulation order = mapper.selectById(id); if (order == null) throw invalid();
        String status = request.status().toUpperCase(Locale.ROOT);
        if (!status.equals("SUCCEEDED") && !status.equals("FAILED")) throw invalid();
        if ("PENDING".equals(order.getStatus())) { order.setStatus(status); mapper.updateById(order); }
        return PaymentSimulationVo.from(order);
    }
    public PaymentSimulationVo get(long id) { SysPaymentSimulation order = mapper.selectById(id); if (order == null) throw invalid(); return PaymentSimulationVo.from(order); }
    private static BusinessException invalid() { return new BusinessException(400, PublicErrorMessage.INVALID_REQUEST); }
}
