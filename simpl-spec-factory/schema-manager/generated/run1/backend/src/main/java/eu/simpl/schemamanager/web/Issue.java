package eu.simpl.schemamanager.web;

/** One structured validation issue (OpenAPI SchemaValidationIssue). */
public record Issue(String code, String description, String location) {
}
