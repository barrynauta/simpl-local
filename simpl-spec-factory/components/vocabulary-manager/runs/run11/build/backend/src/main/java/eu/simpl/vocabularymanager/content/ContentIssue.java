package eu.simpl.vocabularymanager.content;

/**
 * A validation error found in the content (D9, D10), with its position in file order.
 *
 * @param detail the decided detail text
 * @param location the decided location text (D65)
 * @param sequence position in file order, used to sort issues
 */
public record ContentIssue(String detail, String location, int sequence) {}
