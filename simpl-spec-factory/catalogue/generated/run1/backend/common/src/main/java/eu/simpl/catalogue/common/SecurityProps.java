package eu.simpl.catalogue.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Token validation settings (D68). The issuer is always the public Keycloak URL;
 * the JWKS is fetched from the internal one.
 */
@ConfigurationProperties(prefix = "simpl.security")
public class SecurityProps {
    /** Public base, for example http://localhost:18080/realms/ */
    private String issuerBase = "http://localhost:18080/realms/";
    /** Internal base, for example http://keycloak:8080/realms/ */
    private String jwksBase = "http://keycloak:8080/realms/";
    /** The realm accepted for ordinary calls. */
    private String ownRealm = "dataspace-a";
    /** Realms whose tokens are validated on federation paths (trust is checked afterwards). */
    private List<String> federationRealms = new ArrayList<>();
    /** Path prefixes that use the federation realms. */
    private List<String> federationPaths = new ArrayList<>();

    public String issuer(String realm) { return issuerBase + realm; }
    public String jwks(String realm) { return jwksBase + realm + "/protocol/openid-connect/certs"; }

    public String getIssuerBase() { return issuerBase; }
    public void setIssuerBase(String v) { issuerBase = v.endsWith("/") ? v : v + "/"; }
    public String getJwksBase() { return jwksBase; }
    public void setJwksBase(String v) { jwksBase = v.endsWith("/") ? v : v + "/"; }
    public String getOwnRealm() { return ownRealm; }
    public void setOwnRealm(String v) { ownRealm = v; }
    public List<String> getFederationRealms() { return federationRealms; }
    public void setFederationRealms(List<String> v) { federationRealms = v; }
    public List<String> getFederationPaths() { return federationPaths; }
    public void setFederationPaths(List<String> v) { federationPaths = v; }
}
