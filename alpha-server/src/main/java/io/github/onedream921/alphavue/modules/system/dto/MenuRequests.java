package io.github.onedream921.alphavue.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class MenuRequests {
    private MenuRequests() { }

    public record Save(Long parentId,
                       @NotBlank @Size(max = 64) String title,
                       @NotBlank @Pattern(regexp = "MENU|BUTTON|DIRECTORY") String menuType,
                       @Size(max = 128) String path,
                       @Size(max = 255) String component,
                       @Size(max = 128) String permission,
                       @Size(max = 64) String icon,
                       Integer sortOrder,
                       Integer visible,
                       Integer status) { }
}
