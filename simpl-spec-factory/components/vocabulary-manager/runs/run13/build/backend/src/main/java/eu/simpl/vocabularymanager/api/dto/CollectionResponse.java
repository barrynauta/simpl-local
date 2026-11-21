package eu.simpl.vocabularymanager.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** Wire form of a page (OpenAPI CollectionPage, D22, D38, D59, D78, D111, D153). Absent links are left out. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CollectionResponse<T>(
        int page,
        int totalPages,
        long total,
        int pageSize,
        String self,
        String first,
        String prev,
        String next,
        String last,
        @JsonInclude(JsonInclude.Include.ALWAYS) List<T> items) {}
