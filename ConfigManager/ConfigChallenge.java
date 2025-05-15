package com.hackathon.deserialization;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigChallenge { 
    private static final Logger LOGGER = Logger.getLogger(ConfigChallenge.class.getName()); 

    /**
     * A distinct serializable class, different from AppConfiguration.
     * Its readObject() method attempts to execute a local command.
     */
    static class AlternateTestObject implements Serializable { 
        private static final long serialVersionUID = 100L;
        String actionDetail; // This will hold the command to be executed

        public AlternateTestObject(String detail) { 
            this.actionDetail = detail;
        }

        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject(); // Deserialize the fields
            System.err.println("\n*********************************************************");
            System.err.println("*** AlternateTestObject.readObject() was invoked. ***"); 
            System.err.println("*** Attempting to execute command: " + actionDetail);
            try {
                // Attempt to execute the command stored in actionDetail
                Process process = Runtime.getRuntime().exec(actionDetail);
                // Optionally, you could wait for the process and check its exit code,
                // but for a simple demonstration, just attempting execution is enough.
                // process.waitFor(); 
                System.err.println("*** Command execution attempted for: " + actionDetail + " (No immediate error, but success depends on OS & command)");
            } catch (IOException e) {
                System.err.println("*** IOException while trying to execute command '" + actionDetail + "': " + e.getMessage());
                LOGGER.log(Level.WARNING, "IOException during AlternateTestObject command execution", e);
            } 
            // catch (InterruptedException e) {
            //     System.err.println("*** InterruptedException while waiting for command '" + actionDetail + "': " + e.getMessage());
            //     Thread.currentThread().interrupt(); // Restore interruption status
            // }
            System.err.println("*********************************************************\n");
        }
    }

    public static void main(String[] args) {
        ConfigurationManager manager = new ConfigurationManager(); 

        // 1. Create and serialize an AppConfiguration
        AppConfiguration originalConfig = new AppConfiguration("API_ENDPOINT", "https://api.example.com/data", 2, true);
        LOGGER.info("Original Config: " + originalConfig);
        String legitimateSerializedConfig = manager.serializeConfiguration(originalConfig);
        if (legitimateSerializedConfig == null) {
            LOGGER.severe("Failed to serialize legitimate config. Exiting.");
            return;
        }
        System.out.println("Serialized Legitimate AppConfiguration (Base64): " + legitimateSerializedConfig);

        // 2. Create and serialize an "AlternateTestObject" with a command
        // Determine a simple command based on OS for demonstration
        String commandToExecute = System.getProperty("os.name").toLowerCase().startsWith("win") ? "notepad.exe" : "gedit"; 
        // If gedit is not available on Linux, a safer command might be "ls" or "echo hello"
        // For a more general test that just prints, one could use:
        // String commandToExecute = "echo AlternateTestObject_was_here";

        AlternateTestObject altObject = new AlternateTestObject(commandToExecute); 
        String alternateSerializedPayload;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(altObject);
            alternateSerializedPayload = Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to create alternate payload.", e);
            return;
        }
        System.out.println("Serialized AlternateTestObject (Base64, command: " + commandToExecute + "): " + alternateSerializedPayload);
        System.out.println("\n---------------------------------------------------------");
        System.out.println("--- TESTING DESERIALIZATION ---");
        System.out.println("---------------------------------------------------------");

        // Test 1: Deserialize legitimate config using the loadConfiguration method
        System.out.println("\n[Test 1] Deserializing legitimate AppConfiguration:");
        AppConfiguration config1 = manager.loadConfiguration(legitimateSerializedConfig); 
        if (config1 != null) {
            System.out.println("  SUCCESS: Legitimate AppConfiguration loaded: " + config1.getConfigName());
        } else {
            System.err.println("  FAILURE: Legitimate AppConfiguration NOT loaded.");
        }

        // Test 2: Deserialize alternate payload using the loadConfiguration method
        System.out.println("\n[Test 2] Deserializing AlternateTestObject:");
        AppConfiguration config2 = manager.loadConfiguration(alternateSerializedPayload); 
        if (config2 != null) {
            System.err.println("  NOTE: AlternateTestObject resulted in an AppConfiguration object (type mismatch).");
        } else {
            System.out.println("  INFO: AlternateTestObject did not result in an AppConfiguration object.");
            System.out.println("  >>>> CHECK CONSOLE for 'AlternateTestObject.readObject() was invoked.' message. <<<<");
            System.out.println("  If seen, the deserialization process invoked methods from the AlternateTestObject, including command execution attempt.");
        }

        System.out.println("\n---------------------------------------------------------");
        System.out.println("Object deserialization process demonstrated.");
        System.out.println("---------------------------------------------------------");
    }
}
