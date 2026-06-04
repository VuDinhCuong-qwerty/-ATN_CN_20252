package com.demo.change.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PageResponse<T> {

    private List<T> content;
    private long totalElement;
    private int totalPage;
    private int currentPage;
    private int pageSize;
}
