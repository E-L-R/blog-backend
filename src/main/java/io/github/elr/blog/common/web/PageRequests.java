package io.github.elr.blog.common.web;

import io.github.elr.blog.common.error.BadRequestException;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Validates client-supplied sorting against an allow-list. Sorting on arbitrary properties would let
 * clients trigger slow, unindexed queries (or probe internal fields), so unknown fields are a 400.
 */
public final class PageRequests {

    public static final int MAX_PAGE_SIZE = 50;

    private PageRequests() {}

    public static Pageable sanitize(Pageable requested, Set<String> allowedSortProperties, Sort defaultSort) {
        for (Sort.Order order : requested.getSort()) {
            if (!allowedSortProperties.contains(order.getProperty())) {
                throw new BadRequestException("Cannot sort by '" + order.getProperty() + "'. Allowed: "
                        + String.join(", ", new TreeSet<>(allowedSortProperties)));
            }
        }
        Sort sort = requested.getSort().isSorted() ? requested.getSort() : defaultSort;
        int size = Math.min(requested.getPageSize(), MAX_PAGE_SIZE);
        return PageRequest.of(requested.getPageNumber(), size, sort);
    }
}
