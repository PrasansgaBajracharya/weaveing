const tabLogin = document.getElementById('tabLogin');
const tabSignup = document.getElementById('tabSignup');
const panelLogin = document.getElementById('panelLogin');
const panelSignup = document.getElementById('panelSignup');
const authHeading = document.getElementById('authHeading');
const authSub = document.getElementById('authSub');

function showLogin() {
    tabLogin.classList.add('active');
    tabSignup.classList.remove('active');
    panelLogin.classList.add('active');
    panelSignup.classList.remove('active');
    authHeading.textContent = 'Welcome back';
    authSub.textContent = 'Log in to keep buying, selling and creating.';
}

function showSignup() {
    tabSignup.classList.add('active');
    tabLogin.classList.remove('active');
    panelSignup.classList.add('active');
    panelLogin.classList.remove('active');
    authHeading.textContent = 'Create your account';
    authSub.textContent = 'Join Nepal’s community of crochet creators.';
}

tabLogin.addEventListener('click', showLogin);
tabSignup.addEventListener('click', showSignup);
document.getElementById('goSignup').addEventListener('click', showSignup);
document.getElementById('goLogin').addEventListener('click', showLogin);

if (window.location.pathname === '/signup') {
    showSignup();
} else {
    showLogin();
}

const suPassword = document.getElementById('suPassword');
const pwStrength = document.getElementById('pwStrength');
const pwStrengthLabel = document.getElementById('pwStrengthLabel');

suPassword.addEventListener('input', () => {
    const value = suPassword.value;

    let level = 0;

    if (value.length >= 6) {
        level = 1;
    }

    if (
        value.length >= 8 &&
        /[0-9]/.test(value) &&
        /[a-zA-Z]/.test(value)
    ) {
        level = 2;
    }

    if (
        value.length >= 10 &&
        /[0-9]/.test(value) &&
        /[A-Z]/.test(value) &&
        /[^a-zA-Z0-9]/.test(value)
    ) {
        level = 3;
    }

    pwStrength.dataset.level = level;

    const labels = [
        'Use 8+ characters with a mix of letters & numbers',
        'Weak password',
        'Good password',
        'Strong password'
    ];

    pwStrengthLabel.textContent = labels[level];
});

const signupForm = document.getElementById('signupForm');

signupForm.addEventListener('submit', function (event) {
    const password = document.getElementById('suPassword').value;
    const confirm = document.getElementById('suConfirm').value;
    const confirmError = document.getElementById('confirmError');

    if (password !== confirm) {
        event.preventDefault();
        confirmError.style.display = 'block';
        return;
    }

    confirmError.style.display = 'none';
});

function setupPasswordToggle(toggleId, inputId) {
    const toggle = document.getElementById(toggleId);
    const input = document.getElementById(inputId);

    if (!toggle || !input) {
        return;
    }

    toggle.addEventListener('click', function () {
        const showingPassword = input.type === 'password';

        input.type = showingPassword ? 'text' : 'password';

        toggle.classList.toggle(
            'is-visible',
            showingPassword
        );

        toggle.setAttribute(
            'aria-label',
            showingPassword ? 'Hide password' : 'Show password'
        );
    });
}

setupPasswordToggle('toggleLoginPassword', 'loginPassword');
setupPasswordToggle('togglePassword', 'suPassword');
setupPasswordToggle('toggleConfirmPassword', 'suConfirm');

const toast = document.getElementById('toast');
const toastMsg = document.getElementById('toastMsg');

function showToast(message) {
    toastMsg.textContent = message;
    toast.classList.add('show');

    setTimeout(() => {
        toast.classList.remove('show');
    }, 2200);
}

const verificationOverlay = document.getElementById('verificationOverlay');
const verificationClose = document.getElementById('verificationClose');
const verificationAction = document.getElementById('verificationAction');
const verificationTitle = document.getElementById('verificationTitle');
const verificationMessage = document.getElementById('verificationMessage');
const verificationEyebrow = document.getElementById('verificationEyebrow');

if (verificationOverlay) {

    const params =
        new URLSearchParams(window.location.search);

    const verificationStatus =
        params.get('verified');

    const loginVerificationStatus =
        params.get('verification');

    if (loginVerificationStatus === 'pending') {

        verificationEyebrow.textContent = 'EMAIL VERIFICATION REQUIRED';

        verificationTitle.textContent = 'Check your email';

        verificationMessage.textContent =
            'Your email has not been verified yet. We have sent a new verification link to your email address. Please verify your account before logging in.';

        verificationAction.textContent = 'Back to Login';
    }

    if (loginVerificationStatus === 'device') {

        verificationEyebrow.textContent = 'NEW DEVICE DETECTED';

        verificationTitle.textContent = 'Check your email';

        verificationMessage.textContent =
            'We noticed a login from a new browser or device. We have sent a verification link to your email. Verify this device before logging in.';

        verificationAction.textContent = 'Back to Login';
    }

    if (loginVerificationStatus === 'device-verified') {

        verificationEyebrow.textContent = 'DEVICE VERIFIED';

        verificationTitle.textContent = 'This device is trusted';

        verificationMessage.textContent =
            'Your browser has been verified and will be remembered for 30 days. You can now log in normally.';

        verificationAction.textContent = 'Continue to Login';
    }

    if (loginVerificationStatus === 'device-verified-once') {

        verificationEyebrow.textContent = 'LOGIN VERIFIED';

        verificationTitle.textContent = 'You are verified';

        verificationMessage.textContent =
            'This login has been verified, but this browser was not saved as a trusted device. You may be asked to verify again the next time you log in from this browser.';

        verificationAction.textContent = 'Continue to Login';
    }

    if (loginVerificationStatus === 'device-failed') {

        verificationOverlay.classList.add('is-failed');

        verificationEyebrow.textContent = 'DEVICE VERIFICATION FAILED';

        verificationTitle.textContent = 'Link expired';

        verificationMessage.textContent =
            'This device verification link is invalid or has expired. Please log in again to receive a new verification link.';

        verificationAction.textContent = 'Back to Login';
    }

    if (verificationStatus === 'failed') {

        verificationOverlay.classList.add('is-failed');

        verificationEyebrow.textContent = 'VERIFICATION FAILED';

        verificationTitle.textContent = 'Something went wrong';

        verificationMessage.textContent =
            'This verification link is invalid or has already been used. You can return to sign up and try again.';

        verificationAction.textContent = 'Back to Sign Up';
    }

    function closeVerificationPopup() {
        verificationOverlay.classList.remove('is-visible');

        setTimeout(() => {
            verificationOverlay.remove();
        }, 180);
    }

    verificationClose.addEventListener(
        'click',
        closeVerificationPopup
    );

    verificationAction.addEventListener(
        'click',
        function () {

            if (verificationStatus === 'failed') {
                window.location.href = '/signup';
                return;
            }

            window.location.href = '/login';
        }
    );

    verificationOverlay.addEventListener(
        'click',
        function (event) {
            if (event.target === verificationOverlay) {
                closeVerificationPopup();
            }
        }
    );

    document.addEventListener(
        'keydown',
        function (event) {
            if (event.key === 'Escape') {
                closeVerificationPopup();
            }
        }
    );

    window.history.replaceState(
        {},
        document.title,
        window.location.pathname
    );
}