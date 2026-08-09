package io.github.onedream921.alphavue.modules.file.controller;

import io.github.onedream921.alphavue.common.api.ApiResponse;
import io.github.onedream921.alphavue.common.api.PageResponse;
import io.github.onedream921.alphavue.framework.web.BaseController;
import io.github.onedream921.alphavue.modules.log.BusinessType;
import io.github.onedream921.alphavue.modules.log.OperationLog;
import io.github.onedream921.alphavue.modules.file.service.FileService;
import io.github.onedream921.alphavue.modules.system.service.SystemAccessService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.MediaType;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件管理接口
 */
@Validated
@Tag(name = "文件管理")
@RestController
@RequestMapping("/api/files")
public class FileController extends BaseController {

    private final FileService fileService;
    private final SystemAccessService access;

    public FileController(FileService fileService, SystemAccessService access) {
        this.fileService = fileService;
        this.access = access;
    }

    /**
     * 上传文件并保存文件元数据
     */
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @OperationLog(module = "File", operation = "Upload file", type = BusinessType.CREATE,
            saveRequest = false, saveResponse = false)
    public ApiResponse<FileService.FileView> upload(@RequestPart("file") MultipartFile file, HttpServletRequest request) {
        access.require("file:upload");
        long uploaderId = loginUserId();
        return success(fileService.upload(file, uploaderId), request);
    }

    /**
     * 分页查询文件元数据列表
     */
    @GetMapping
    public ApiResponse<PageResponse<FileService.FileView>> page(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            HttpServletRequest request) {
        access.require("file:list");
        return success(fileService.page(page, size), request);
    }

    /**
     * 为私有文件生成新的短期访问地址，避免持久化或复用已过期的签名地址。
     */
    @GetMapping("/{id}/access-url")
    public ApiResponse<String> accessUrl(@PathVariable @Positive long id, HttpServletRequest request) {
        access.require("file:list");
        return success(fileService.accessUrl(id), request);
    }

    /**
     * 删除文件对象并软删除对应元数据
     */
    @DeleteMapping("/{id}")
    @OperationLog(module = "File", operation = "Delete file", type = BusinessType.DELETE,
            saveRequest = false, saveResponse = false)
    public ApiResponse<Void> delete(@PathVariable @Positive long id, HttpServletRequest request) {
        access.require("file:delete");
        fileService.delete(id);
        return success(request);
    }

    /**
     * 使用短期签名读取私有文件，不要求浏览器额外注入 Authorization 请求头。
     */
    @Hidden
    @GetMapping("/{id}/content")
    public ResponseEntity<InputStreamResource> content(@PathVariable @Positive long id,
                                                        @RequestParam long expires,
                                                        @RequestParam String signature) {
        FileService.FileContent content = fileService.openForAccess(id, expires, signature);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(new InputStreamResource(content.input()));
    }
}
