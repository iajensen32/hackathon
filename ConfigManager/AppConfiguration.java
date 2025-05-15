package com.hackathon.deserialization;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Date;

/**
 * Represents a simple application configuration that can be serialized.
 */
public class AppConfiguration implements Serializable {
    private static final long serialVersionUID = 1L; // Standard practice

    private String configName;
    private String configValue;
    private int version;
    private boolean active;
    private Date lastUpdated;

    public AppConfiguration(String configName, String configValue, int version, boolean active) {
        this.configName = configName;
        this.configValue = configValue;
        this.version = version;
        this.active = active;
        this.lastUpdated = new Date();
    }

    // Getters
    public String getConfigName() { return configName; }
    public String getConfigValue() { return configValue; }
    public int getVersion() { return version; }
    public boolean isActive() { return active; }
    public Date getLastUpdated() { return lastUpdated; }

    // Setters (optional, but can be useful)
    public void setConfigName(String configName) { this.configName = configName; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }
    public void setVersion(int version) { this.version = version; }
    public void setActive(boolean active) { this.active = active; }
    public void setLastUpdated(Date lastUpdated) { this.lastUpdated = lastUpdated; }


    @Override
    public String toString() {
        return "AppConfiguration{" +
               "configName='" + configName + '\'' +
               ", configValue='" + configValue + '\'' +
               ", version=" + version +
               ", active=" + active +
               ", lastUpdated=" + lastUpdated +
               '}';
    }

    // Custom readObject to show when deserialization occurs
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        System.out.println("AppConfiguration.readObject() called for: " + this.configName);
    }
}
