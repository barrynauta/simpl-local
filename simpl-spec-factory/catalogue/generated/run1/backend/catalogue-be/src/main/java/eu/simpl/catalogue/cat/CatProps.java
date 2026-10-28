package eu.simpl.catalogue.cat;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/** Settings of catalogue-be. */
@ConfigurationProperties(prefix = "simpl.cat")
public class CatProps {
    private String qualityStubUrl = "http://quality-stub:8000";
    private List<String> seedDirs = new ArrayList<>();
    private String keyRegistry;

    public String getQualityStubUrl() { return qualityStubUrl; }
    public void setQualityStubUrl(String v) { qualityStubUrl = v.endsWith("/") ? v.substring(0, v.length() - 1) : v; }
    public List<String> getSeedDirs() { return seedDirs; }
    public void setSeedDirs(List<String> v) { seedDirs = v; }
    public String getKeyRegistry() { return keyRegistry; }
    public void setKeyRegistry(String v) { keyRegistry = v; }
}
