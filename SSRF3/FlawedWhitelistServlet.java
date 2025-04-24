// Assume necessary imports for IO, Net are present
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

// Imports for Servlet API (use javax.* or jakarta.* depending on your environment)
import javax.servlet.ServletException;
// import javax.servlet.annotation.WebServlet; // Optional: for mapping if not using web.xml
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Simplified Servlet demonstrating an SSRF vulnerability hidden behind FLAWED validation.
 * The validation attempts a whitelist but is easily bypassed.
 * Assumes deployment within a Servlet container like Apache Tomcat.
 * Intended for educational/hackathon purposes ONLY. Do NOT use in production.
 */
// Optional: Define servlet mapping via annotation or web.xml
// @WebServlet("/FlawedWhitelistServlet") // Example mapping
public class FlawedWhitelistServlet extends HttpServlet {

    // Consider making timeouts configurable
    private static final int CONNECT_TIMEOUT_MS = 5000; // 5 seconds
    private static final int READ_TIMEOUT_MS = 10000;  // 10 seconds

    /**
     * Performs FLAWED validation on the target URL string.
     * Tries to whitelist specific domains but uses String.contains(), making it bypassable.
     *
     * @param urlString The URL string to validate.
     * @return true if the URL passes the flawed checks, false otherwise.
     */
    private static boolean isValidTarget(String urlString) {
        System.out.println("[DEBUG] Attempting flawed whitelist validation for URL: " + urlString);
        if (urlString == null || urlString.trim().isEmpty()) {
            System.out.println("[DEBUG] Validation FAILED: URL is null or empty.");
            return false;
        }

        // --- FLAWED WHITELIST VALIDATION using String.contains() ---
        // GOAL: Appear to only allow "app1.example.com" or "app2.example.com".
        // FLAW: Uses .contains() on the entire URL string, not just the host part.
        //       This means the check passes if the allowed domain appears anywhere
        //       (e.g., subdomain, path, query parameter), allowing an attacker
        //       to control the actual host the connection goes to.
        //
        // Bypass Examples:
        // 1. Attacker Controls Subdomain: Provide urlString = "http://app1.example.com.attacker.site/"
        //    -> .contains("app1.example.com") passes.
        //    -> Actual connection target: app1.example.com.attacker.site
        // 2. Allowed Domain in Query Parameter: Provide urlString = "http://attacker.site/?get=app1.example.com"
        //    -> .contains("app1.example.com") passes.
        //    -> Actual connection target: attacker.site
        // 3. Allowed Domain in Path: Provide urlString = "http://attacker.site/info/app2.example.com/"
        //    -> .contains("app2.example.com") passes.
        //    -> Actual connection target: attacker.site
        //
        // RESULT: This validation is ineffective; any domain can be targeted by crafting the URL string correctly.

        String lowerCaseUrl = urlString.toLowerCase(); // Use lowercase for case-insensitive check

        boolean appearsAllowed = lowerCaseUrl.contains("app1.example.com") ||
                                 lowerCaseUrl.contains("app2.example.com");

        if (!appearsAllowed) {
            // If neither required substring is found anywhere, the check fails.
            System.out.println("[DEBUG] Validation FAILED: URL string does not contain 'app1.example.com' or 'app2.example.com'.");
            return false;
        }

        // If either "app1.example.com" or "app2.example.com" is found *somewhere*
        // in the string, this flawed check considers it valid.
        System.out.println("[DEBUG] Validation PASSED (flawed check - required substring found, actual host NOT validated).");
        return true;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String responseBody = "";
        int responseStatusCode = HttpServletResponse.SC_OK; // Default to 200 OK
        String responseContentType = "text/plain; charset=utf-8"; // Default response type

        // 1. Get the target URL from the request parameter
        String resourceTarget = request.getParameter("resourceTarget");

        // 2. Perform FLAWED Validation
        if (resourceTarget == null || resourceTarget.trim().isEmpty()) {
            responseStatusCode = HttpServletResponse.SC_BAD_REQUEST; // 400
            responseContentType = "application/json; charset=utf-8";
            responseBody = "{\"error\": \"Missing 'resourceTarget' query parameter.\"}";
        } else if (!isValidTarget(resourceTarget)) { // <<< Call the flawed validation routine
            // Handle validation failure (because the required substring wasn't found)
            responseStatusCode = HttpServletResponse.SC_BAD_REQUEST; // 400
            responseContentType = "application/json; charset=utf-8";
            responseBody = "{\"error\": \"Invalid 'resourceTarget' parameter. Target failed validation check (does not appear to be an allowed domain).\"}"; // Updated error msg slightly
            System.out.println("[WARN] Request blocked by flawed validation for target: " + resourceTarget);
        } else {
            // --- Validation Passed (Due to Flaws) - Proceed with SSRF Vulnerability ---
            try {
                System.out.println("[INFO] Flawed validation passed. Proceeding to fetch: " + resourceTarget);

                // *** VULNERABILITY ***
                // Still creating URL from user input, the validation was ineffective.
                URL targetUrl = new URL(resourceTarget);

                // Minimal protocol check (could be added to validation, but kept here for clarity)
                String protocol = targetUrl.getProtocol().toLowerCase();
                if (!protocol.equals("http") && !protocol.equals("https")) {
                    throw new MalformedURLException("Disallowed protocol: '" + protocol + "'. Only http/https allowed.");
                }

                // Make the connection FROM THE SERVER side to the ACTUAL host parsed by new URL()
                HttpURLConnection connection = (HttpURLConnection) targetUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);
                connection.setInstanceFollowRedirects(false);

                // Execute the request
                int targetStatusCode = connection.getResponseCode();
                responseStatusCode = targetStatusCode;

                String targetContentType = connection.getContentType();
                if (targetContentType != null && !targetContentType.isEmpty()) {
                    responseContentType = targetContentType;
                }

                InputStream inputStream = (targetStatusCode >= 200 && targetStatusCode < 400)
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                if (inputStream != null) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                        responseBody = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                    }
                } else {
                     responseBody = "{\"message\": \"Received status " + targetStatusCode + " from target with no response body.\"}";
                     if (!responseContentType.startsWith("application/json")) {
                         responseContentType = "application/json; charset=utf-8";
                     }
                }
                System.out.println("[INFO] Servlet successfully proxied (after flawed validation): " + resourceTarget + " | Status: " + targetStatusCode);

            } catch (MalformedURLException e) {
                System.err.println("[WARN] Invalid URL or disallowed protocol: " + resourceTarget + " (" + e.getMessage() + ")");
                responseStatusCode = HttpServletResponse.SC_BAD_REQUEST; // 400
                responseContentType = "application/json; charset=utf-8";
                responseBody = "{\"error\": \"Invalid 'resourceTarget' URL or disallowed protocol.\", \"detail\": \"" + escapeJson(e.getMessage()) + "\"}";
            } catch (UnknownHostException e) {
                 System.err.println("[WARN] Unknown host: " + resourceTarget + " (" + e.getMessage() + ")");
                 responseStatusCode = HttpServletResponse.SC_BAD_GATEWAY; // 502
                 responseContentType = "application/json; charset=utf-8";
                 responseBody = "{\"error\": \"Could not resolve host for target resource.\", \"detail\": \"" + escapeJson(e.getMessage()) + "\"}";
            } catch (IOException e) {
                System.err.println("[ERROR] IO Error fetching target: " + resourceTarget + " (" + e.toString() + ")");
                responseStatusCode = HttpServletResponse.SC_BAD_GATEWAY; // 502
                responseContentType = "application/json; charset=utf-8";
                responseBody = "{\"error\": \"Could not connect to or read from the target resource.\", \"detail\": \"" + escapeJson(e.toString()) + "\"}";
            } catch (Exception e) {
                System.err.println("[ERROR] Unexpected error fetching target: " + resourceTarget + " (" + e.toString() + ")");
                responseStatusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR; // 500
                responseContentType = "application/json; charset=utf-8";
                responseBody = "{\"error\": \"An unexpected server error occurred.\", \"detail\": \"" + escapeJson(e.toString()) + "\"}";
            }
            // --- End SSRF Vulnerability Point ---
        }

        // 3. Send the response back to the original client
        response.setStatus(responseStatusCode);
        response.setContentType(responseContentType);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");

        if (responseBody != null && !responseBody.isEmpty()) {
            byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
            response.setContentLength(responseBytes.length);
            try (OutputStream os = response.getOutputStream()) {
                os.write(responseBytes);
            }
        } else {
             response.setContentLength(0);
        }
    }

    // Basic JSON string escaping helper
    private static String escapeJson(String text) {
        if (text == null) return null;
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\b", "\\b")
                   .replace("\f", "\\f")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}