package io.github.onedream921.alphavue.common.api;

import java.util.List;

/**
 * Immutable payload for page-based collection responses.
 *
 * @param records page contents
 * @param total total number of matching records
 * @param page one-based page number
 * @param size requested page size
 */
public record PageResponse<T>(List<T> records, long total, int page, int size) {

    public PageResponse {
        records = List.copyOf(records);
    }
}
