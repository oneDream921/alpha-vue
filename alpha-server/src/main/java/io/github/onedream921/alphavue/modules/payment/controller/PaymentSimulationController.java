package io.github.onedream921.alphavue.modules.payment.controller;

import io.github.onedream921.alphavue.common.api.ApiResponse;
import io.github.onedream921.alphavue.framework.web.BaseController;
import io.github.onedream921.alphavue.modules.log.BusinessType;
import io.github.onedream921.alphavue.modules.log.OperationLog;
import io.github.onedream921.alphavue.modules.payment.dto.PaymentSimulationRequests;
import io.github.onedream921.alphavue.modules.payment.service.PaymentSimulationService;
import io.github.onedream921.alphavue.modules.payment.vo.PaymentSimulationVo;
import io.github.onedream921.alphavue.modules.system.service.SystemAccessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system/settings/pay/simulated-orders")
public class PaymentSimulationController extends BaseController {
    private final PaymentSimulationService service; private final SystemAccessService access;
    public PaymentSimulationController(PaymentSimulationService service, SystemAccessService access) { this.service = service; this.access = access; }
    @PostMapping @OperationLog(module="System", operation="Create simulated payment", type=BusinessType.CREATE, saveRequest=false, saveResponse=false)
    public ApiResponse<PaymentSimulationVo> create(@Valid @RequestBody PaymentSimulationRequests.Create body, HttpServletRequest request) { access.require("system:setting:update"); return success(service.create(body), request); }
    @PostMapping("/{id}/complete") @OperationLog(module="System", operation="Complete simulated payment", type=BusinessType.UPDATE, saveRequest=false, saveResponse=false)
    public ApiResponse<PaymentSimulationVo> complete(@PathVariable @Positive long id, @Valid @RequestBody PaymentSimulationRequests.Complete body, HttpServletRequest request) { access.require("system:setting:update"); return success(service.complete(id, body), request); }
    @GetMapping("/{id}") public ApiResponse<PaymentSimulationVo> get(@PathVariable @Positive long id, HttpServletRequest request) { access.require("system:setting:list"); return success(service.get(id), request); }
}
