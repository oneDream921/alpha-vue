package io.github.onedream921.alphavue.modules.system.settings.controller;

import io.github.onedream921.alphavue.common.api.ApiResponse;
import io.github.onedream921.alphavue.framework.web.BaseController;
import io.github.onedream921.alphavue.modules.log.BusinessType;
import io.github.onedream921.alphavue.modules.log.OperationLog;
import io.github.onedream921.alphavue.modules.system.service.SystemAccessService;
import io.github.onedream921.alphavue.modules.system.settings.SettingGroup;
import io.github.onedream921.alphavue.modules.system.settings.dto.SystemSettingRequests;
import io.github.onedream921.alphavue.modules.system.settings.service.SystemSettingService;
import io.github.onedream921.alphavue.modules.system.settings.vo.SystemSettingVo;
import io.github.onedream921.alphavue.modules.wechat.service.OfficialAccountService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;

@Tag(name = "系统配置")
@RestController
@RequestMapping("/api/system/settings")
public class SystemSettingController extends BaseController {
    private final SystemSettingService service;
    private final SystemAccessService access;
    private final OfficialAccountService officialAccountService;
    public SystemSettingController(SystemSettingService service, SystemAccessService access, OfficialAccountService officialAccountService) { this.service = service; this.access = access; this.officialAccountService = officialAccountService; }

    @GetMapping("/public")
    public ApiResponse<Map<String, Object>> publicSettings(HttpServletRequest request) { return success(service.publicSettings(), request); }

    @GetMapping("/file/credentials")
    @OperationLog(module = "System", operation = "Reveal file storage credentials", type = BusinessType.OTHER, saveRequest = false, saveResponse = false)
    public ApiResponse<SystemSettingService.FileStorageCredentials> fileStorageCredentials(HttpServletRequest request) {
        access.require("system:setting:update");
        return success(service.fileStorageCredentials(), request);
    }

    @GetMapping("/{group}")
    public ApiResponse<SystemSettingVo> get(@PathVariable String group, HttpServletRequest request) {
        access.require("system:setting:list"); return success(service.get(parse(group)), request);
    }

    @PutMapping("/{group}")
    @OperationLog(module = "System", operation = "Update system setting", type = BusinessType.UPDATE, saveRequest = false, saveResponse = false)
    public ApiResponse<SystemSettingVo> save(@PathVariable String group, @Valid @RequestBody SystemSettingRequests.Save body, HttpServletRequest request) {
        access.require("system:setting:update"); return success(service.save(parse(group), body), request);
    }

    @PostMapping("/security/keys/regenerate")
    @OperationLog(module = "System", operation = "Regenerate system RSA keys", type = BusinessType.UPDATE, saveRequest = false, saveResponse = false)
    public ApiResponse<SystemSettingService.RsaKeyPair> regenerateRsaKeys(HttpServletRequest request) {
        access.require("system:setting:update");
        return success(service.regenerateRsaKeys(), request);
    }

    @PostMapping("/official-account/menu/publish")
    @OperationLog(module = "System", operation = "Publish official account menu", type = BusinessType.UPDATE, saveRequest = false, saveResponse = false)
    public ApiResponse<Void> publishOfficialAccountMenu(HttpServletRequest request) {
        access.require("system:setting:update");
        officialAccountService.publishMenu();
        return success(request);
    }

    private static SettingGroup parse(String value) { try { return SettingGroup.parse(value); } catch (IllegalArgumentException exception) { throw new io.github.onedream921.alphavue.common.exception.BusinessException(400, io.github.onedream921.alphavue.common.exception.PublicErrorMessage.INVALID_REQUEST); } }
}
