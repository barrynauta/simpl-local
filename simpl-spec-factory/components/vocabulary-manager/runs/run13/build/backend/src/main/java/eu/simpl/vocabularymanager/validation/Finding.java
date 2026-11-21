package eu.simpl.vocabularymanager.validation;

/**
 * A validation error before it becomes a problem issue.
 *
 * @param position file position used to order the report (D125)
 */
public record Finding(int position, String detail, String location) {}
