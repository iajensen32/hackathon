$(document).ready(function () {
    // Validation function
    function validateInput(input) {
        const invalidCharacters = /[^a-zA-Z0-9_#]/g; // Regex to detect invalid characters
        if (invalidCharacters.test(input)) {
            alert('Input contains invalid characters. Only alphanumeric, underscore, and pound sign are allowed.');
            return false; // Return false if invalid characters are found
        }
        return true; // Return true if input is valid
    }

    function sanitizeInput(input) {
        return input.replace(/<script>/gi, '').replace(/<script>/gi, '');
    }
   

    // Handle sign-in form submission
    $('#signInForm').on('submit', function (event) {
        event.preventDefault();

        const username = $('#username').val().trim();
        const password = $('#password').val().trim();

        // Validate inputs
        if (!validateInput(username)) {
            return; // Stop submission if username is invalid
        }
        if (!validateInput(password)) {
            return; // Stop submission if password is invalid
        }

        if (!username) {
            alert('Please enter your username.');
            return;
        }

        if (!password) {
            alert(`${username}, you need to submit a password.`);
            return;
        }

        // Redirect to the same page with the username as a URL parameter
        const currentUrl = window.location.href.split('?')[0]; // Remove existing query parameters
        const newUrl = `${currentUrl}?username=${username}`;
        window.location.href = newUrl; // Redirect to the new URL
    });

    // Display the welcome message if the username is in the URL
    function displayWelcomeMessage() {
        const params = new URLSearchParams(window.location.search);
        const username = params.get('username'); // Get the 'username' parameter from the URL
        if (username) {
            const sanitizedUsername = sanitizeInput(username);
            const welcomeMessage = `Welcome ${sanitizedUsername}`;
            document.querySelector(".create-account").innerHTML += `<p>${welcomeMessage}</p>`;
        }
    }

    // Call the function to display the welcome message
    displayWelcomeMessage();

    // Show cookie consent banner
    function showCookieConsent() {
        const consentBanner = document.createElement("div");
        consentBanner.id = "cookieConsentBanner";
        consentBanner.style.position = "fixed";
        consentBanner.style.bottom = "0";
        consentBanner.style.width = "100%";
        consentBanner.style.backgroundColor = "#333";
        consentBanner.style.color = "#fff";
        consentBanner.style.padding = "15px";
        consentBanner.style.textAlign = "center";
        consentBanner.style.zIndex = "1000";
        consentBanner.classList.add("cookie-consent-banner");

        const consentText = document.createElement("p");
        consentText.style.margin = "0";
        consentText.style.fontSize = "14px";
        consentText.textContent = "This website uses cookies to improve your experience, personalize content, and analyze traffic. By continuing to use this website, you consent to our use of cookies. You can manage your cookie preferences and learn more about our cookie usage by visiting our ";

        const privacyLink = document.createElement("a");
        privacyLink.href = "/privacy-policy";
        privacyLink.style.color = "#4CAF50";
        privacyLink.style.textDecoration = "underline";
        privacyLink.textContent = "Privacy Policy";

        consentText.appendChild(privacyLink);
        consentBanner.appendChild(consentText);

        const acceptButton = document.createElement("button");
        acceptButton.id = "acceptCookies";
        acceptButton.style.marginTop = "10px";
        acceptButton.style.padding = "10px 20px";
        acceptButton.style.backgroundColor = "#4CAF50";
        acceptButton.style.color = "#fff";
        acceptButton.style.border = "none";
        acceptButton.style.borderRadius = "5px";
        acceptButton.style.cursor = "pointer";
        acceptButton.textContent = "Accept Cookies";

        consentBanner.appendChild(acceptButton);

        document.body.appendChild(consentBanner);

        // Handle "Accept Cookies" button click
        document.getElementById("acceptCookies").addEventListener("click", function () {
            setCookie("cookie_consent", "accepted", 365); // Save consent for 1 year
            document.body.removeChild(consentBanner);
            initializeTracking(); // Initialize tracking after consent
        });
    }

    // Set a cookie
    function setCookie(name, value, days) {
        const date = new Date();
        date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000)); // Convert days to milliseconds
        const expires = "expires=" + date.toUTCString();
        document.cookie = `${name}=${value};${expires};path=/;SameSite=Lax`;
    }

    // Get a cookie
    function getCookie(name) {
        const nameEQ = name + "=";
        const cookies = document.cookie.split(';');
        for (let i = 0; i < cookies.length; i++) {
            let cookie = cookies[i].trim();
            if (cookie.indexOf(nameEQ) === 0) {
                return cookie.substring(nameEQ.length, cookie.length);
            }
        }
        return null;
    }

    // Initialize tracking
    function initializeTracking() {
        // Check if the tracking cookie already exists
        if (!getCookie("tracking_cookie")) {
            // Set the tracking cookie if it doesn't exist
            setCookie("tracking_cookie", crypto.randomUUID(), 7); // Example value "12345", expires in 7 days
        }
    }

    // Check if the user has already accepted cookies
    if (!getCookie("cookie_consent")) {
        showCookieConsent();
    } else {
        initializeTracking(); // Initialize tracking if consent is already given
    }
});

