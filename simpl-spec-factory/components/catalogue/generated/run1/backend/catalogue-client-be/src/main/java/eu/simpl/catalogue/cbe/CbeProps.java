package eu.simpl.catalogue.cbe;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings of the catalogue client backend. */
@ConfigurationProperties(prefix = "simpl.cbe")
public class CbeProps {
    private String qmaUrl = "http://qma-a:8000/v1";
    private String catUrl = "http://cat-a:8000";
    private String schemaStubUrl = "http://schema-stub:8000";
    private String qualityStubUrl = "http://quality-stub:8000";
    private String federationFile = "/config/federation.json";

    private static String trim(String v) { return v.endsWith("/") ? v.substring(0, v.length() - 1) : v; }

    public String getQmaUrl() { return qmaUrl; }
    public void setQmaUrl(String v) { qmaUrl = trim(v); }
    public String getCatUrl() { return catUrl; }
    public void setCatUrl(String v) { catUrl = trim(v); }
    public String getSchemaStubUrl() { return schemaStubUrl; }
    public void setSchemaStubUrl(String v) { schemaStubUrl = trim(v); }
    public String getQualityStubUrl() { return qualityStubUrl; }
    public void setQualityStubUrl(String v) { qualityStubUrl = trim(v); }
    public String getFederationFile() { return federationFile; }
    public void setFederationFile(String v) { federationFile = v; }
}
