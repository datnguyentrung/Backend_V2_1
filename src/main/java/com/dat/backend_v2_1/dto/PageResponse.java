package com.dat.backend_v2_1.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PageResponse<T> {
    List<T> content;
    int pageNumber;
    int pageSize;
    long totalElements;
    int totalPages;
    boolean first;
    boolean last;
    boolean empty;

    /**
     * Hàm Static Factory 1: Convert trực tiếp khi không cần map dữ liệu
     * (Ví dụ: Page<DTO> -> PageResponse<DTO>)
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();
    }

    /**
     * Hàm Static Factory 2: Tự động map Entity sang DTO
     * (Ví dụ: Page<Entity> -> PageResponse<DTO>)
     * * @param page Dữ liệu Page gốc từ Database (chứa Entity)
     *
     * @param mapper Hàm chuyển đổi từ Entity (E) sang DTO (T)
     */
    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        // Tự động duyệt qua list Entity và map sang list DTO
        List<T> dtoList = page.getContent().stream()
                .map(mapper)
                .toList();

        return PageResponse.<T>builder()
                .content(dtoList)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();
    }
}