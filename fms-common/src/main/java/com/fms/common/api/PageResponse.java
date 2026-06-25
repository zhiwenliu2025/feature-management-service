package com.fms.common.api;

import java.util.List;

public record PageResponse<T>(
        List<T> data,
        Pagination pagination
) {
    public record Pagination(
            String nextCursor,
            boolean hasMore,
            Long totalCount
    ) {
        public static Pagination empty() {
            return new Pagination(null, false, 0L);
        }

        public static Pagination of(String nextCursor, boolean hasMore, long totalCount) {
            return new Pagination(nextCursor, hasMore, totalCount);
        }
    }
}
