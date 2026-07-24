package io.github.onedream921.alphavue.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class DeptRequests {
    private DeptRequests() { }

    public record Save(Long parentId,
                       @NotBlank @Size(max = 64) String name,
                       Integer sortOrder,
                       Integer status) { }
}
