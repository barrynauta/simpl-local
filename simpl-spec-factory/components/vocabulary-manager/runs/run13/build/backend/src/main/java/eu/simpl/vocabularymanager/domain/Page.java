package eu.simpl.vocabularymanager.domain;

import java.util.List;

/** One page of a list read from the store with its total (D22, D85). */
public record Page<T>(List<T> items, long total) {

    public Page {
        items = List.copyOf(items);
    }
}
