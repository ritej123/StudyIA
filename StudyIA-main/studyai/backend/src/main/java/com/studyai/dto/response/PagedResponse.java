package com.studyai.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Réponse générique pour les données paginées.
 *
 * @param <T> Type d'élément dans la page.
 */
@Data
@Builder
public class PagedResponse<T> {
    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean last;
}
