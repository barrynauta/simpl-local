package eu.simpl.catalogue.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/** Static federation configuration of digest section 6.3. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FederationConfig {
    public static final String LOCAL_ALIAS = "local";
    public static final Pattern CATALOGUE_ID = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");

    public String gaId;
    public Catalogue ownCatalogue;
    public Client outboundClient;
    public Client serviceClient;
    public List<FederatedCatalogue> federatedCatalogues = new ArrayList<>();
    public List<TrustedAuthority> trustedAuthorities = new ArrayList<>();

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Catalogue {
        public String id;
        public String name;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Client {
        public String tokenUrl;
        public String clientId;
        public String clientSecret;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FederatedCatalogue {
        public String id;
        public String name;
        public String gaId;
        public String authorityURL;
        public boolean active;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AttributeMapping {
        public String remoteAuthorityAttribute;
        public String localAuthorityAttribute;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TrustedAuthority {
        public String gaId;
        public String issuer;
        public String clientId;
        public List<AttributeMapping> attributeMapping = new ArrayList<>();
        public List<String> additionalIdentityAttributes = new ArrayList<>();
    }

    public static FederationConfig load(String path) {
        try {
            return Json.MAPPER.readValue(new File(path), FederationConfig.class);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read federation configuration " + path, e);
        }
    }

    public boolean isOwn(String id) {
        return LOCAL_ALIAS.equals(id) || (ownCatalogue != null && ownCatalogue.id.equals(id));
    }

    public Optional<FederatedCatalogue> federated(String id) {
        return federatedCatalogues.stream().filter(c -> c.id.equals(id)).findFirst();
    }

    public Optional<TrustedAuthority> trusted(String issuer, String azp) {
        return trustedAuthorities.stream()
                .filter(t -> t.issuer != null && t.issuer.equals(issuer) && t.clientId != null && t.clientId.equals(azp))
                .findFirst();
    }

    /** Attribute mapping of D37: replace mapped codes, drop unmapped ones, add the additional attributes. */
    public static List<String> mapAttributes(TrustedAuthority ta, List<String> incoming) {
        List<String> out = new ArrayList<>();
        for (String code : incoming) {
            for (AttributeMapping m : ta.attributeMapping) {
                if (code.equals(m.remoteAuthorityAttribute) && !out.contains(m.localAuthorityAttribute)) {
                    out.add(m.localAuthorityAttribute);
                }
            }
        }
        for (String a : ta.additionalIdentityAttributes) if (!out.contains(a)) out.add(a);
        return out;
    }

    public static List<String> splitAttributes(String header) {
        List<String> out = new ArrayList<>();
        if (header == null) return out;
        for (String p : header.split(",")) {
            String t = p.trim();
            if (!t.isEmpty() && !out.contains(t)) out.add(t);
        }
        return out;
    }
}
