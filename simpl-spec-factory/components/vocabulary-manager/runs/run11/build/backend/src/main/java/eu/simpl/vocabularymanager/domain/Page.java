package eu.simpl.vocabularymanager.domain;

import java.util.List;

/** One page of a store query and the total count. */
public record Page<T>(List<T> items, long total) {

    public Page {
        items = List.copyOf(items);
    }
}
