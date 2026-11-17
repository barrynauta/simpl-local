package eu.simpl.catalogue.common;

import java.util.List;

/** Role sets of digest section 5.4. */
public final class Roles {
    public static final String CATALOGUE_SEARCHER = "CATALOGUE_SEARCHER";
    public static final String DATA_PUBLISHER = "DATA_PROVIDER_PUBLISHER";
    public static final String APP_PUBLISHER = "APP_PROVIDER_PUBLISHER";
    public static final String INFRA_PUBLISHER = "INFRA_PROVIDER_PUBLISHER";
    public static final String GA_SCHEMA_VIEWER = "GA_SCHEMA_VIEWER";
    public static final String GA_SCHEMA_ADMIN = "GA_SCHEMA_ADMIN";

    public static final List<String> PUBLISHER = List.of(DATA_PUBLISHER, APP_PUBLISHER, INFRA_PUBLISHER);
    public static final List<String> SEARCHER = List.of(CATALOGUE_SEARCHER, DATA_PUBLISHER, APP_PUBLISHER, INFRA_PUBLISHER);
    public static final List<String> SCHEMA_READ = List.of(GA_SCHEMA_VIEWER, GA_SCHEMA_ADMIN);

    private Roles() {}
}
