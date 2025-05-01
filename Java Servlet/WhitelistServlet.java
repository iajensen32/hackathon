
/**
 * Intended for educational/hackathon purposes ONLY. Do NOT use in production.
 */
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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WhitelistServlet extends HttpServlet {

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 10000;

/*   private static boolean isValidTarget(String urlString) create a secure isValidTarget method that prevents SSRF attacks, allow only whitelisted domains, 
* the white list is app1.example.com and app2.example.com. If IP address are used instead of domain names, * the request should be blocked. The whitelist should be configurable, and the servlet should not allow any other domains or IP addresses.
*/
    private static boolean isValidTarget(String urlString) {
        // Example whitelist check (this should be replaced with a more secure implementation)
        return urlString != null && (urlString.contains("app1.example.com") || urlString.contains("app2.example.com"));
    }       
                                       
 
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String responseBody = "";
        int responseStatusCode = HttpServletResponse.SC_OK;
        String responseContentType = "text/plain; charset=utf-8";

        String resourceTarget = request.getParameter("resourceTarget");

        if (resourceTarget == null || resourceTarget.trim().isEmpty()) {
            responseStatusCode = HttpServletResponse.SC_BAD_REQUEST;
            responseContentType = "application/json; charset=utf-8";
            responseBody = "{\"error\": \"Missing 'resourceTarget' query parameter.\"}";
        } else if (!isValidTarget(resourceTarget)) {
            responseStatusCode = HttpServletResponse.SC_BAD_REQUEST;
            responseContentType = "application/json; charset=utf-8";
            responseBody = "{\"error\": \"Invalid 'resourceTarget' parameter. Target failed validation check (does not appear to be an allowed domain).\"}";
        } else {
            try {
                URL targetUrl = new URL(resourceTarget);
                String protocol = targetUrl.getProtocol().toLowerCase();
                if (!protocol.equals("http") && !protocol.equals("https")) {
                    throw new MalformedURLException("Disallowed protocol: '" + protocol + "'. Only http/https allowed.");
                }

                HttpURLConnection connection = (HttpURLConnection) targetUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);
                connection.setInstanceFollowRedirects(false);

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

            } catch (MalformedURLException e) {
                responseStatusCode = HttpServletResponse.SC_BAD_REQUEST;
                responseContentType = "application/json; charset=utf-8";
                responseBody = "{\"error\": \"Invalid 'resourceTarget' URL or disallowed protocol.\", \"detail\": \"" + escapeJson(e.getMessage()) + "\"}";
            } catch (UnknownHostException e) {
                responseStatusCode = HttpServletResponse.SC_BAD_GATEWAY;
                responseContentType = "application/json; charset=utf-8";
                responseBody = "{\"error\": \"Could not resolve host for target resource.\", \"detail\": \"" + escapeJson(e.getMessage()) + "\"}";
            } catch (IOException e) {
                responseStatusCode = HttpServletResponse.SC_BAD_GATEWAY;
                responseContentType = "application/json; charset=utf-8";
                responseBody = "{\"error\": \"Could not connect to or read from the target resource.\", \"detail\": \"" + escapeJson(e.toString()) + "\"}";
            } catch (Exception e) {
                responseStatusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                responseContentType = "application/json; charset=utf-8";
                responseBody = "{\"error\": \"An unexpected server error occurred.\", \"detail\": \"" + escapeJson(e.toString()) + "\"}";
            }
        }

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