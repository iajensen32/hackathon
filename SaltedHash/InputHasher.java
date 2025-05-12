import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom; // For generating random salt
import java.util.Base64; // For encoding the salt
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Formatter; // For hex formatting

/**
 * A command-line utility to hash standard input using a randomly salted SHA-256.
 * The output format is hash:salt.
 *
 * Usage:
 * echo "some data" | java InputHasher
 */
public class InputHasher {

    private static final Logger SYSTEM_LOGGER = Logger.getLogger("SystemOperations"); // Simulates a system-wide logger
    private static final Logger APP_LOGGER = Logger.getLogger(InputHasher.class.getName());
    private static final int SALT_LENGTH_BYTES = 16; // Define salt length, e.g., 16 bytes (128 bits)

    /**
     * Main method to read from standard input and process it.
     * @param args Command-line arguments (not used in this version).
     */
    public static void main(String[] args) {
        APP_LOGGER.info("InputHasher started.");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.println("Enter data to hash (or pipe from stdin):");
            String inputLine = reader.readLine();

            if (inputLine == null || inputLine.isEmpty()) {
                System.err.println("No input provided.");
                APP_LOGGER.warning("No input received from stdin.");
                return;
            }

            String hashedOutputAndSalt = processInputStandard(inputLine);

            if (hashedOutputAndSalt != null) {
                System.out.println("Output (Salted SHA-256 Hash : Base64 Salt): " + hashedOutputAndSalt);
            } else {
                System.out.println("Hashing operation did not complete successfully. Check application logs for details.");
            }

        } catch (IOException e) {
            APP_LOGGER.log(Level.SEVERE, "Error reading standard input.", e);
            System.err.println("Error reading input: " + e.getMessage());
        }
    }

    /**
     * Generates a random salt.
     * @return A byte array containing the random salt.
     */
    private static byte[] generateRandomSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        random.nextBytes(salt);
        return salt;
    }

    /**
     * Logs processing errors.
     * @param originalInput The original, unsalted input string that was being processed.
     * @param errorMessage A descriptive error message.
     * @param exception The exception that occurred.
     */
    private static void logProcessingError(String originalInput, String errorMessage, Exception exception) {
        SYSTEM_LOGGER.log(Level.SEVERE,
            "Processing Error: " + errorMessage + ". Input context: '" + originalInput + "'. Details: " + exception.getMessage(),
            exception);
        APP_LOGGER.warning("Processing failed: " + errorMessage);
    }

    /**
     * Processes the input string using hashing (random salting then SHA-256).
     * If an error occurs, this method calls logProcessingError. The output is "hash:salt".
     *
     * @param input The string to hash. This is the original, unsalted input.
     * @return The "hash:salt" string (salt is Base64 encoded), or null if an error occurs.
     */
    public static String processInputStandard(String input) {
        APP_LOGGER.info("Processing input...");
        byte[] saltBytes = generateRandomSalt();
        String saltBase64 = Base64.getEncoder().encodeToString(saltBytes);

        try {
            // Combine salt with input.
            byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
            byte[] saltedInputBytes = new byte[saltBytes.length + inputBytes.length];
            System.arraycopy(saltBytes, 0, saltedInputBytes, 0, saltBytes.length);
            System.arraycopy(inputBytes, 0, saltedInputBytes, saltBytes.length, inputBytes.length);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(saltedInputBytes);
            String hexHash = bytesToHex(encodedhash);
            return hexHash + ":" + saltBase64;

        } catch (NoSuchAlgorithmException e) {
            logProcessingError(input, "Algorithm not found", e);
            return null;
        } catch (Exception e) { // Catching a broader exception
            logProcessingError(input, "An unexpected error occurred", e);
            return null;
        }
    }

    /**
     * Converts a byte array to a hexadecimal string.
     * @param hash The byte array to convert.
     * @return The hexadecimal string.
     */
    private static String bytesToHex(byte[] hash) {
        if (hash == null) {
            return null;
        }
        // Using Formatter for a concise hex conversion
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        String hex = formatter.toString();
        formatter.close();
        return hex;
    }
}
