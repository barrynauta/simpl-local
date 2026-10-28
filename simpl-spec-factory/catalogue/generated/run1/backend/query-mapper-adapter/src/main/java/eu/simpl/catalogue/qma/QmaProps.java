package eu.simpl.catalogue.qma;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings of the query mapper adapter. */
@ConfigurationProperties(prefix = "simpl.qma")
public class QmaProps {
    private String catUrl = "http://cat-a:8000";
    private String federationFile = "/config/federation.json";
    private int federationTimeoutSeconds = 3;
    private int backendTimeoutSeconds = 10;

    public String getCatUrl() { return catUrl; }
    public void setCatUrl(String v) { catUrl = v.endsWith("/") ? v.substring(0, v.length() - 1) : v; }
    public String getFederationFile() { return federationFile; }
    public void setFederationFile(String v) { federationFile = v; }
    public int getFederationTimeoutSeconds() { return federationTimeoutSeconds; }
    public void setFederationTimeoutSeconds(int v) { federationTimeoutSeconds = v; }
    public int getBackendTimeoutSeconds() { return backendTimeoutSeconds; }
    public void setBackendTimeoutSeconds(int v) { backendTimeoutSeconds = v; }
}
