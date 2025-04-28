// script.js

document.addEventListener('DOMContentLoaded', () => {

    // --- Get references to elements ---
    const welcomeContainer = document.getElementById('welcomeMessageContainer');
    const errorContainer = document.getElementById('errorMessageContainer');
    const form = document.getElementById('signInForm');
    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    const submitButton = form ? form.querySelector('button[type="submit"]') : null;

    // --- Deceptive Username Validation Function ---
    /**
     * Checks if the username format is valid.
     * Attempts to block direct script tags, but may have flaws regarding event handlers or other vectors.
     * @param {string} input - The username string to validate.
     * @returns {boolean} - True if the format seems valid according to the rules, false otherwise.
     */
    function isValidUsername(input) {
        // Updated Regex: Block only <script...> tags (case-insensitive).
        // This is intentionally weaker and allows other vectors like onerror/onload.
        const invalidPattern = /<script.*?>/i; // i for case-insensitive

        if (invalidPattern.test(input)) {
            console.log("Username validation failed due to script tag pattern.");
             // Display specific error for script tags
             if(errorContainer) {
                errorContainer.textContent = 'Script tags are not allowed in username.';
                errorContainer.classList.add('error-active');
             }
            return false; // Found script tag pattern
        }

        // Add a basic length check to make it seem more complete
        if (input.length < 3 || input.length > 30) {
             console.log("Username validation failed due to length constraints.");
             // Display length error specifically
             if(errorContainer) {
                errorContainer.textContent = 'Username must be between 3 and 30 characters.';
                errorContainer.classList.add('error-active');
             }
             return false;
        }


        // Add character set validation
        // Adjusted to allow characters needed for the intended XSS payload, including parentheses.
        const allowedCharsPattern = /^[a-zA-Z0-9_<>="' /()]+$/; // Added () to allowed chars
         if (!allowedCharsPattern.test(input)) {
             console.log("Username validation failed due to invalid characters.");
              // Display char error specifically
             if(errorContainer) {
                errorContainer.textContent = 'Username contains invalid characters.';
                errorContainer.classList.add('error-active');
             }
             return false;
         }


        // If none of the checks failed, consider it valid *by these rules*
        return true;
    }


    // --- Part 1: Handle Displaying Welcome Message on Page Load (XSS Vulnerability) ---
    const urlParams = new URLSearchParams(window.location.search);
    const usernameFromUrl = urlParams.get('username');

    if (usernameFromUrl && welcomeContainer) {
        const decodedUsername = decodeURIComponent(usernameFromUrl);

        // Clear previous errors before validation
         if(errorContainer) {
            errorContainer.textContent = '';
            errorContainer.classList.remove('error-active');
        }

        // Apply the NEW validation function
        if (isValidUsername(decodedUsername)) {
            // *** XSS VULNERABILITY VIA URL PARAMETER STILL EXISTS HERE ***
            // If the input bypasses the flawed isValidUsername, it gets injected.
            // Example bypass: <img src=x onerror=alert('XSS')>
            welcomeContainer.innerHTML = `Welcome ${decodedUsername}!`;
            console.log(`Displayed welcome message for username from URL: ${decodedUsername}`);
        } else {
            // Validation failed (either by pattern, length, or characters)
            console.log("isValidUsername check blocked display from URL.");
            // Error message might have already been set by isValidUsername function
            if (welcomeContainer && (!errorContainer || !errorContainer.classList.contains('error-active'))) {
                 welcomeContainer.textContent = "Welcome! (Invalid username format detected in URL)";
            } else if (welcomeContainer) {
                 welcomeContainer.textContent = "Please sign in."; // Default if error shown
            }
        }
    } else if (welcomeContainer) {
        welcomeContainer.textContent = 'Please sign in.';
    }

    // --- Part 2: Handle Form Submission with XHR ---
    if (form && usernameInput && passwordInput && errorContainer && submitButton) {
        form.addEventListener('submit', (event) => {
            event.preventDefault();

            // Clear previous error messages and disable button
            errorContainer.textContent = '';
            errorContainer.classList.remove('error-active');
            submitButton.disabled = true;
            submitButton.textContent = 'Signing In...';

            const usernameValue = usernameInput.value.trim(); // Trim here
            const passwordValue = passwordInput.value.trim();

            // --- Empty Field Validation Check ---
            if (usernameValue === '' || passwordValue === '') {
                console.error('Validation failed: Username or password empty.');
                errorContainer.textContent = 'Please enter both username and password.';
                errorContainer.classList.add('error-active');
                submitButton.disabled = false;
                submitButton.textContent = 'Sign In';
                return;
            }

            // --- Perform Username Validation on Submit as well ---
            // Use the non-trimmed value for validation if needed, but trimmed makes more sense
            if (!isValidUsername(usernameValue)) {
                 // isValidUsername might have already set a specific error message
                 if (!errorContainer.classList.contains('error-active')) {
                    // Set a generic error if specific one wasn't set by validator
                    errorContainer.textContent = 'Invalid username format.';
                    errorContainer.classList.add('error-active');
                 }
                 submitButton.disabled = false;
                 submitButton.textContent = 'Sign In';
                 return; // Stop submission if username format is invalid
            }


            // --- If Validations Pass, Update Welcome Message & Send XHR ---
            console.log('Validation passed, updating welcome message and preparing XHR request...');
            // Use textContent for safety when updating from form input directly
            welcomeContainer.textContent = `Welcome ${usernameValue}! (Attempting login...)`;

            const xhr = new XMLHttpRequest();
            const url = 'https://app1.com/login';
            // Send the trimmed username value
            const data = JSON.stringify({ username: usernameValue, password: passwordValue });

            xhr.open('POST', url, true);
            xhr.setRequestHeader('Content-Type', 'application/json;charset=UTF-8');

            xhr.onload = function () {
                submitButton.disabled = false;
                submitButton.textContent = 'Sign In';

                if (xhr.status >= 200 && xhr.status < 300) {
                    console.log('XHR Success:', xhr.responseText);
                    welcomeContainer.textContent = `Welcome ${usernameValue}! (Login attempt finished)`;
                    form.reset();
                } else {
                    console.error('XHR Error - Status:', xhr.status, xhr.statusText);
                    errorContainer.textContent = `Login attempt failed. Server responded with status: ${xhr.status}`;
                    errorContainer.classList.add('error-active');
                }
            };

            xhr.onerror = function () {
                submitButton.disabled = false;
                submitButton.textContent = 'Sign In';
                console.error('XHR Request failed (Network error)');
                errorContainer.textContent = 'Login request failed. Check your network connection.';
                errorContainer.classList.add('error-active');
            };

            console.log(`Sending POST request to ${url} with data: ${data}`);
            xhr.send(data);

        });
    } else {
        console.error('Required form elements not found.');
    }
});
