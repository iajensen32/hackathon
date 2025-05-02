package com.example.loans.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID; // For generating tokens

/**
 * Servlet to handle updating loan interest rates.
 * Assumes user is already authenticated by the container or a filter.
 * Includes CSRF token handling logic.
 * The doGet method generates the form with the token.
 * The doPost method processes the form submission.
 */
@WebServlet("/updateRate")
public class UpdateRateServlet extends HttpServlet {

    // Key used to store the token in the session
    private static final String CSRF_TOKEN_SESSION_ATTR = "csrfToken";
    // Name of the parameter expected in the request
    private static final String CSRF_TOKEN_REQUEST_PARAM = "csrfToken";

    /**
     * Handles GET requests. Generates and displays the HTML form
     * for updating the loan rate, including the current CSRF token.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Ensure session exists and user is authenticated (basic check)
        HttpSession session = request.getSession(true); // Get or create session
        // In a real app, check authentication status more robustly
        if (session.isNew() && request.getUserPrincipal() == null) {
             // If it's a new session and no principal, likely not logged in
             // Redirect to login or show error. For demo, we'll proceed but note it.
             System.out.println("[SERVLET-GET] New session created, user likely not authenticated yet.");
             // We still generate a token for the new session
        } else {
             System.out.println("[SERVLET-GET] Existing session detected.");
        }

        // 2. Get or generate the CSRF token for the session
        String token = ensureCsrfToken(session);

        // 3. Prepare the response
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        // 4. Generate the HTML form dynamically
        out.println("<!DOCTYPE html>");
        out.println("<html lang=\"en\">");
        out.println("<head>");
        out.println("    <meta charset=\"UTF-8\">");
        out.println("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        out.println("    <title>Update Loan Interest Rate</title>");
        // Include the same CSS as the static HTML file for consistency
        out.println("    <style>");
        out.println("        body { font-family: sans-serif; padding: 20px; line-height: 1.6; }");
        out.println("        .container { max-width: 500px; margin: auto; padding: 20px; border: 1px solid #ccc; border-radius: 8px; background-color: #f9f9f9; }");
        out.println("        label { display: block; margin-bottom: 5px; font-weight: bold; }");
        out.println("        input[type=\"text\"], input[type=\"number\"] { width: calc(100% - 22px); padding: 8px; margin-bottom: 15px; border: 1px solid #ccc; border-radius: 4px; }");
        out.println("        input[type=\"submit\"] { background-color: #28a745; color: white; padding: 10px 15px; border: none; border-radius: 4px; cursor: pointer; font-size: 1em; }");
        out.println("        input[type=\"submit\"]:hover { background-color: #218838; }");
        out.println("        .note { font-size: 0.9em; color: #555; margin-top: 15px; }");
        out.println("    </style>");
        out.println("</head>");
        out.println("<body>");
        out.println("    <div class=\"container\">");
        out.println("        <h1>Update Car Loan Interest Rate</h1>");
        out.println("        <p>Enter the Loan ID and the new Annual Percentage Rate (APR).</p>");

        // Form submits to the same servlet URL (/updateRate) via POST
        out.println("        <form action=\"updateRate\" method=\"post\">");
        out.println("            <div>");
        out.println("                <label for=\"loanId\">Loan ID:</label>");
        out.println("                <input type=\"text\" id=\"loanId\" name=\"loanId\" pattern=\"[A-Z0-9]{8}\" title=\"Enter an 8-character alphanumeric Loan ID\" required>");
        out.println("            </div>");
        out.println("            <div>");
        out.println("                <label for=\"newRate\">New Interest Rate (%):</label>");
        out.println("                <input type=\"text\" id=\"newRate\" name=\"newRate\" pattern=\"\\d+(\\.\\d{1,3})?\" title=\"Enter rate like 4.5 or 3.75\" required>");
        out.println("            </div>");

        // --- Embed the CSRF token in a hidden field ---
        out.println("            <input type=\"hidden\" name=\"" + CSRF_TOKEN_REQUEST_PARAM + "\" value=\"" + escapeHtml(token) + "\">");
        // ---------------------------------------------

        out.println("            <div>");
        out.println("                <input type=\"submit\" value=\"Update Rate\">");
        out.println("            </div>");
        out.println("        </form>");
        out.println("         <p class=\"note\">Ensure you are logged in before submitting.</p>");
        out.println("    </div>");
        out.println("</body>");
        out.println("</html>");

        out.close(); // Close the writer
    }


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        HttpSession session = request.getSession(false); // Get existing session

        out.println("<!DOCTYPE html><html><head><title>Rate Update Status</title></head><body>");
        out.println("<h1>Rate Update Status</h1>");

        // 1. Check if session exists
        if (session == null) {
             System.out.println("[SERVLET-POST] No active session found. Cannot process request.");
             out.println("<p style='color:red;'>Error: No active session. Please log in.</p>");
             out.println("</body></html>");
             return;
        }
        // Ensure token exists in session for the authenticated user
        ensureCsrfToken(session);
        System.out.println("[SERVLET-POST] Active session detected.");

        // 2. Get parameters from the request
        String loanId = request.getParameter("loanId");
        String newRateStr = request.getParameter("newRate");

        // Basic validation of parameters
        if (loanId == null || loanId.trim().isEmpty() || newRateStr == null || newRateStr.trim().isEmpty()) {
            out.println("<p style='color:red;'>Error: Loan ID and New Rate are required.</p>");
            out.println("</body></html>");
            return;
        }

        // 3. Validate CSRF Token using the helper method
        boolean proceed = validateCsrfToken(request, session);

        // 4. Perform the action ONLY if checks passed or were bypassed
        if (proceed) {
            try {
                // Attempt to parse the rate
                double newRate = Double.parseDouble(newRateStr.trim());

                // Simulate updating the loan rate in a backend system
                String principalName = (request.getUserPrincipal() != null) ? request.getUserPrincipal().getName() : "[Unknown Authenticated User]";
                System.out.println("[SERVLET-POST] >>> ACTION: Updating loan '" + escapeHtml(loanId) + "' requested by '" + principalName + "' to new rate: " + newRate + "% <<<");

                // Display success message
                out.println("<p style='color:green;'>Successfully updated interest rate for Loan ID: "
                            + escapeHtml(loanId) + " to " + newRate + "%.</p>");
                out.println("<p>(Action performed by authenticated user: " + escapeHtml(principalName) + ")</p>");

            } catch (NumberFormatException e) {
                out.println("<p style='color:red;'>Error: Invalid format for New Rate. Please enter a number (e.g., 4.5).</p>");
            } catch (Exception e) {
                out.println("<p style='color:red;'>An unexpected error occurred while updating the rate: " + escapeHtml(e.getMessage()) + "</p>");
                System.err.println("[SERVLET-POST] Error during simulated rate update: " + e.getMessage());
            }
        } else {
            // Token validation failed (and token was present)
             out.println("<p style='color:red;'>Error: Security token validation failed. Please try submitting the form again.</p>");
        }

        // Link back to the form (which will be regenerated by doGet)
        out.println("<hr><p><a href='updateRate'>Update another rate</a></p>");
        out.println("</body></html>");
        out.close();
    }

    /**
     * Validates the CSRF token present in the request against the one in the session.
     *
     * @param request The HttpServletRequest.
     * @param session The HttpSession (assumed non-null).
     * @return true if the request should proceed (token valid), false otherwise (token invalid).
     */
    private boolean validateCsrfToken(HttpServletRequest request, HttpSession session) {
        String requestToken = request.getParameter(CSRF_TOKEN_REQUEST_PARAM);
        String sessionToken = (String) session.getAttribute(CSRF_TOKEN_SESSION_ATTR);

        if (requestToken != null) {
            // --- Path taken if csrfToken parameter IS PRESENT ---
            System.out.println("[CSRF-VALIDATOR] Received request token parameter. Performing check...");
            System.out.println("[CSRF-VALIDATOR] Request Token: " + requestToken);
            System.out.println("[CSRF-VALIDATOR] Session Token: " + sessionToken);

            // Validate the token if it was provided
            if (sessionToken != null && sessionToken.equals(requestToken)) {
                System.out.println("[CSRF-VALIDATOR] CSRF token check PASSED.");
                // Regenerate token after successful validation and state change (good practice)
                generateCsrfToken(session);
                System.out.println("[CSRF-VALIDATOR] Regenerated session CSRF token.");
                return true; // Token is valid
            } else {
                System.out.println("[CSRF-VALIDATOR] CSRF token check FAILED (Mismatch or session token missing).");
                return false; // Token is invalid
            }
        } else {
        
            System.out.println("[CSRF-VALIDATOR] Request token parameter ('" + CSRF_TOKEN_REQUEST_PARAM + "') is MISSING.");
            return true; 
        }
    }


    /**
     * Ensures a CSRF token exists in the session, generating one if needed.
     * @param session The HttpSession. Assumed to be non-null.
     * @return The current or newly generated token.
     */
    private String ensureCsrfToken(HttpSession session) {
        String token = (String) session.getAttribute(CSRF_TOKEN_SESSION_ATTR);
        if (token == null) {
            token = generateCsrfToken(session);
        }
        return token;
    }

    /**
     * Generates a new CSRF token and stores it in the session.
     * @param session The HttpSession. Assumed to be non-null.
     * @return The generated token.
     */
    private String generateCsrfToken(HttpSession session) {
        String token = UUID.randomUUID().toString();
        session.setAttribute(CSRF_TOKEN_SESSION_ATTR, token);
        System.out.println("[SERVLET-UTIL] Generated/Set session CSRF token: " + token);
        return token;
    }

    /** Basic HTML escaping utility. */
    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }
}
