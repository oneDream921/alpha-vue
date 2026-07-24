package io.github.onedream921.alphavue.modules.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** One-based collection page request with a bounded result size. */
public record PageQuery(@Min(1) int page, @Min(1) @Max(100) int size) {
}
