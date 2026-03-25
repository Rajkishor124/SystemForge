package com.systemforge.backend.common.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Standard pagination response wrapper for all list endpoints.
 *
 * <p>Wraps Spring Data's {@link Page} into a client-friendly structure,
 * decoupling internal pagination mechanics from the API contract.
 *
 * <p>Usage in service layer:
 * <pre>{@code
 *   Page<User> page = userRepository.findAll(pageable);
 *   return PagedResponse.from(page, userMapper::toDto);
 * }</pre>
 */
@Getter
@Builder
public class PagedResponse<T> {

    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean first;
    private final boolean last;
    private final boolean empty;

    /**
     * Constructs a {@link PagedResponse} from a Spring Data {@link Page},
     * applying the provided mapper function to each element.
     */
    public static <E, D> PagedResponse<D> from(Page<E> page, java.util.function.Function<E, D> mapper) {
        return PagedResponse.<D>builder()
                .content(page.getContent().stream().map(mapper).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();
    }
}