import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie; // Import Cookie class
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest; // Import MessageDigest
import java.security.NoSuchAlgorithmException; // Import NoSuchAlgorithmException
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64; // Import Base64
import org.mindrot.jbcrypt.BCrypt; // Import BCrypt
import java.util.UUID; // Import UUID

/**
 * Servlet implementation class LoginServlet
 * Handles user login attempts via POST request.
 * Validates credentials against a database using configuration from environment variables.
 * Sets a custom session tracking cookie upon successful login.
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // --- Database Configuration (Loaded from Environment Variables) ---
    private String dbUrl;
    private String dbUser;
    private String dbPassword;

    // A fixed string used in session token generation
    private static final String SESSION_TOKEN_KEY = "a3f9c8d4e7b1a2f3";

    /**
     * Initializes the servlet and loads database configuration from environment variables.
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        dbUrl = System.getenv("DB_CONNECTION_URL");
        dbUser = System.getenv("DB_APP_USER");
        dbPassword = System.getenv("DB_APP_PASSWORD");

        if (dbUrl == null || dbUrl.trim().isEmpty()) {
            throw new ServletException("Database URL environment variable 'DB_CONNECTION_URL' not set.");
        }
        if (dbUser == null || dbUser.trim().isEmpty()) {
            System.err.println("Warning: Database User environment variable 'DB_APP_USER' not set or empty.");
        }
        if (dbPassword == null) {
            System.err.println("Warning: Database Password environment variable 'DB_APP_PASSWORD' not set.");
            dbPassword = "";
        }

        System.out.println("LoginServlet initialized. DB URL loaded from environment variable.");
    }

    /**
     * Handles the HTTP POST method for login attempts.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String csrfToken = request.getParameter("csrfToken");
        HttpSession session = request.getSession(false);
        if (session == null || !csrfToken.equals(session.getAttribute("csrfToken"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }

        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=missing");
            return;
        }

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        boolean loginSuccess = false;
        String userId = null;

        try {
            conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

            // Query to retrieve user details for authentication
            String sql = "SELECT user_id, password FROM users WHERE username = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedPasswordHash = rs.getString("password"); // Retrieve hashed password
                userId = rs.getString("user_id");

                // Compare submitted password with stored hashed password using bcrypt
                if (BCrypt.checkpw(password, storedPasswordHash)) {
                    loginSuccess = true;
                    System.out.println("User '" + username + "' successfully authenticated.");
                } else {
                    System.out.println("User '" + username + "' authentication failed: Incorrect password.");
                }
            } else {
                System.out.println("User '" + username + "' authentication failed: User not found.");
            }

        } catch (SQLException e) {
            System.err.println("Database error during login for user '" + username + "': " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=db");
            return;
        } catch (Exception e) {
            System.err.println("General error during login: " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=unknown");
            return;
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) { /* ignored */ }
            try { if (pstmt != null) pstmt.close(); } catch (SQLException e) { /* ignored */ }
            try { if (conn != null) conn.close(); } catch (SQLException e) { /* ignored */ }
        }

        // Handle login success or failure
        if (loginSuccess) {
            // --- Custom Session Token Generation ---
            String userSessionToken = generateUserSessionToken(username); // Renamed method call
            System.out.println("Generated session token for user '" + username + "': " + userSessionToken);

            // Create a custom cookie to store the session token
            Cookie sessionCookie = new Cookie("userSessionToken", userSessionToken); // Renamed cookie
            sessionCookie.setPath(request.getContextPath() + "/"); // Make cookie available to entire app context

            // Enable HttpOnly and Secure flags for production cookies
            sessionCookie.setHttpOnly(true);
            sessionCookie.setSecure(request.isSecure()); // Ensures Secure flag is set only if the request is over HTTPS

            response.setHeader("Set-Cookie", sessionCookie.getName() + "=" + sessionCookie.getValue() + "; HttpOnly; Secure; SameSite=Strict");

            response.addCookie(sessionCookie); // Add the custom cookie to the response

            // --- Standard Servlet Session Usage (Optional) ---
            // Use the standard session for storing server-side user details.
            session = request.getSession(true); // Get or create standard session
            session.setAttribute("username", username);
            session.setAttribute("userId", userId);
            System.out.println("Standard session created/retrieved for user: " + username + ", ID: " + session.getId());

            // Generate and store CSRF token in session
            String csrfTokenNew = UUID.randomUUID().toString();
            session.setAttribute("csrfToken", csrfTokenNew);

            // Redirect to the authenticated page
            response.sendRedirect(request.getContextPath() + "/authenticated");

        } else {
            // Authentication failed
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=invalid");
        }
    }

    /**
     * @param username The username of the logged-in user.
     * @return A Base64 encoded session token string.
     */
    private String generateUserSessionToken(String username) { // Renamed method
        try {
            // Combine username, timestamp, and application key
            long timestamp = System.currentTimeMillis();
            String rawTokenData = username + ":" + timestamp + ":" + SESSION_TOKEN_KEY;

            // Use MD5 hash algorithm
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(rawTokenData.getBytes(StandardCharsets.UTF_8));

            // Encode the hash using Base64
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);

        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error generating session token: MD5 algorithm not found.");
            // Fallback representation
            return "fallback_" + username + "_" + System.currentTimeMillis();
        }
    }

    /**
     * Called by the servlet container to indicate to a servlet that the
     * servlet is being taken out of service.
     */
    @Override
    public void destroy() {
        System.out.println("LoginServlet destroyed.");
        super.destroy();
    }
}
