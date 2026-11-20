package eu.simpl.vocabularymanager.store;

import java.time.Instant;

/** The data of a vocabulary version to store. */
public record NewVersion(
        String name, String description, String changelog, String createdBy, byte[] content, Instant now) {}
