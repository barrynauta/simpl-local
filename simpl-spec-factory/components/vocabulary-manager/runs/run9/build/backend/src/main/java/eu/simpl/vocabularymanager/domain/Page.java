package eu.simpl.vocabularymanager.domain;

import java.util.List;

/**
 * One page of a collection.
 *
 * @param items the items of the page
 * @param total total number of matching items
 * @param <T> item type
 */
public record Page<T>(List<T> items, long total) {}
