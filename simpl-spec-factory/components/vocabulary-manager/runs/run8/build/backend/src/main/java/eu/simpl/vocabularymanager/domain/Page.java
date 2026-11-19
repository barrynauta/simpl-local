package eu.simpl.vocabularymanager.domain;

import java.util.List;

/**
 * One page of a store query (D22, D85).
 *
 * @param items the items of the page
 * @param total the number of matching items
 * @param <T> item type
 */
public record Page<T>(List<T> items, long total) {}
