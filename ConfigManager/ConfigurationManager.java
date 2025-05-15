package com.hackathon.deserialization;

import java.io.*;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.hackathon.deserialization.AppConfiguration;

/**
 * Manages the serialization and deserialization of AppConfiguration objects.
 */
public class ConfigurationManager {
    private static final Logger LOGGER = Logger.getLogger(ConfigurationManager.class.getName());

    /**
     * Serializes an AppConfiguration object to a Base64 encoded string.
     */
    public String serializeConfiguration(AppConfiguration config) {
        if (config == null) {
            LOGGER.warning("Cannot serialize null configuration.");
            return null;
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(config);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error during serialization", e);
            return null;
        }
    }

    /**
     * Deserializes an AppConfiguration from a Base64 string using direct ObjectInputStream.
     * This method relies on standard Java deserialization mechanisms.
     * Consideration should be given to the source and content of the serialized data.
     *
     * @param base64Config The Base64 encoded serialized AppConfiguration.
     * @return AppConfiguration object or null if an error occurs.
     */
    public AppConfiguration loadConfiguration(String base64Config) { 
        if (base64Config == null || base64Config.isEmpty()) {
            LOGGER.warning("Cannot deserialize null or empty data.");
            return null;
        }
        byte[] data;
        try {
            data = Base64.getDecoder().decode(base64Config);
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, "Base64 decoding failed.", e);
            return null;
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bais)) { // Standard deserialization stream
            Object obj = ois.readObject();
            if (obj instanceof AppConfiguration) {
                return (AppConfiguration) obj;
            } else {
                LOGGER.warning("Deserialized object is not an AppConfiguration. Type: " + (obj != null ? obj.getClass().getName() : "null"));
                return null;
            }
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Deserialization error. Input may be corrupted or of an unexpected format.", e);
            return null;
        }
    }
}
