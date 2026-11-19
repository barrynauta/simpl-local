package eu.simpl.vocabularymanager.domain;

import java.util.List;

/**
 * One page of a stored collection.
 *
 * @param items the items of the page
 * @param total the number of items in the whole collection
 * @param <T> the item type
 */
public record Page<T>(List<T> items, long total) {}
