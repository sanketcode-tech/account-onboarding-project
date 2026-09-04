// ============================================================
// Northbridge Bank Onboarding - Frontend Application
// ============================================================

// ============================================================
// Configuration
// ============================================================

const API_BASE = {
    auth: 'http://localhost:8081/api/auth',
    application: 'http://localhost:8082/api/applications',
    status: 'http://localhost:8083/api/status',
    document: 'http://localhost:8084/api/documents',
};

let jwtToken = null;
let currentApplicationId = null;

// ============================================================
// Initialization
// ============================================================

document.addEventListener('DOMContentLoaded', () => {
    const storedToken = sessionStorage.getItem('jwtToken');
    if (storedToken) {
        jwtToken = storedToken;
        updateNavigation();
        showStatusPage();
    } else {
        showLoginPage();
    }

    // Event listeners
    document.getElementById('nav-home')?.addEventListener('click', showLoginPage);
    document.getElementById('nav-apply')?.addEventListener('click', showApplyPage);
    document.getElementById('nav-status')?.addEventListener('click', showStatusPage);
    document.getElementById('nav-logout')?.addEventListener('click', logout);
});

// ============================================================
// Page Rendering
// ============================================================

function showPage(templateId) {
    const container = document.getElementById('app-container');
    const template = document.getElementById(templateId);
    if (template) {
        container.innerHTML = template.innerHTML;
        attachEventListeners();
    }
}

function showLoginPage() {
    showPage('login-page');
    document.getElementById('login-form')?.addEventListener('submit', handleLogin);
}

function showApplyPage() {
    if (!jwtToken) {
        alert('Please login first');
        showLoginPage();
        return;
    }
    showPage('apply-page');
    document.getElementById('application-form')?.addEventListener('submit', handleApplicationSubmit);
}

function showStatusPage() {
    if (!jwtToken) {
        alert('Please login first');
        showLoginPage();
        return;
    }
    showPage('status-page');
    loadApplicationStatus();
}

// ============================================================
// Event Handlers
// ============================================================

async function handleLogin(event) {
    event.preventDefault();
    const username = document.getElementById('username')?.value || document.getElementById('email')?.value;
    const password = document.getElementById('password')?.value;

    if (!username || !password) {
        alert('Please fill in all fields');
        return;
    }

    try {
        const res = await window.apiClient.post(window.apiClient.defaults.auth + '/login', { username, password });
        if (res.ok) {
            const data = await res.json();
            const token = data.token || data.jwtToken;
            if (!token) { alert('Login failed: no token'); return; }
            if (window.auth && typeof window.auth.setToken === 'function') {
                window.auth.setToken(token);
            } else {
                sessionStorage.setItem('jwtToken', token);
            }
            jwtToken = sessionStorage.getItem('jwtToken');
            updateNavigation();
            showApplyPage();
            console.log('✓ Login successful');
        } else {
            alert('Invalid credentials');
        }
    } catch (error) {
        console.error('Login error:', error);
        alert('Login failed. Please try again.');
    }
}
async function handleApplicationSubmit(event) {
    event.preventDefault();

    const applicationData = {
        customerId: 'cust-' + Date.now(), // Generate simple ID for demo
        personalDetails: {
            fullName: document.getElementById('fullName')?.value,
            dob: document.getElementById('dob')?.value,
            panNumber: document.getElementById('panNumber')?.value,
            address: document.getElementById('address')?.value,
        },
        employmentDetails: {
            occupation: document.getElementById('occupation')?.value,
            annualIncome: parseFloat(document.getElementById('annualIncome')?.value),
            employerName: document.getElementById('employerName')?.value,
        },
        accountPreferences: {
            branch: document.getElementById('branch')?.value,
            initialDeposit: parseFloat(document.getElementById('initialDeposit')?.value),
            debitCardRequired: document.getElementById('debitCard')?.checked,
        },
    };

    try {
        const response = await window.apiClient.post(API_BASE.application, applicationData);

        if (response.ok) {
            const data = await response.json();
            currentApplicationId = data.applicationId;
            alert('Application submitted successfully!');
            console.log('✓ Application submitted:', data);
            showStatusPage();
        } else {
            alert('Failed to submit application');
        }
    } catch (error) {
        console.error('Application submission error:', error);
        alert('Error submitting application');
    }
}

async function loadApplicationStatus() {
    if (!currentApplicationId) {
        document.getElementById('status-content').innerHTML = '<p>No active application</p>';
        return;
    }

    try {
        const response = await window.apiClient.get(`${API_BASE.status}/${currentApplicationId}`);

        if (response.ok) {
            const data = await response.json();
            renderStatusTimeline(data);
        } else {
            document.getElementById('status-content').innerHTML = '<p>Could not load status</p>';
        }
    } catch (error) {
        console.error('Status loading error:', error);
        document.getElementById('status-content').innerHTML = '<p>Error loading status</p>';
    }
}

// ============================================================
// UI Helpers
// ============================================================

function updateNavigation() {
    const loginLink = document.getElementById('nav-login');
    const logoutBtn = document.getElementById('nav-logout');

    if (jwtToken) {
        if (loginLink) loginLink.style.display = 'none';
        if (logoutBtn) logoutBtn.style.display = 'block';
    } else {
        if (loginLink) loginLink.style.display = 'block';
        if (logoutBtn) logoutBtn.style.display = 'none';
    }
}

function logout() {
    jwtToken = null;
    sessionStorage.removeItem('jwtToken');
    updateNavigation();
    showLoginPage();
    console.log('✓ Logged out');
}

function renderStatusTimeline(applicationData) {
    const statusMap = {
        SUBMITTED: { step: 1, label: 'Application Submitted' },
        UNDER_REVIEW: { step: 2, label: 'Under Review' },
        OFFER_READY: { step: 3, label: 'Offer Ready' },
        OFFER_ACCEPTED: { step: 4, label: 'Offer Accepted' },
        AWAITING_SIGNATURE: { step: 5, label: 'Awaiting Signature' },
        SIGNED: { step: 6, label: 'Document Signed' },
        PROVISIONING: { step: 7, label: 'Provisioning Account' },
        ACTIVE: { step: 8, label: 'Account Activated' },
        DECLINED: { step: -1, label: 'Application Declined' },
    };

    const currentStatus = applicationData.status || 'SUBMITTED';
    const currentStep = statusMap[currentStatus]?.step || 1;

    let timeline = '<div class="timeline">';
    for (const [status, info] of Object.entries(statusMap)) {
        if (info.step > 0) {
            const isCompleted = info.step <= currentStep;
            const markerClass = isCompleted ? 'completed' : '';
            timeline += `
                <div class="timeline-item">
                    <div class="timeline-marker ${markerClass}">${info.step}</div>
                    <div class="timeline-content">
                        <h3>${info.label}</h3>
                        <p>${isCompleted ? '✓ Completed' : 'Pending'}</p>
                    </div>
                </div>
            `;
        }
    }
    timeline += '</div>';

    document.getElementById('status-content').innerHTML = `
        <div>
            <p><strong>Application ID:</strong> ${applicationData.applicationId}</p>
            <p><strong>Current Status:</strong> ${currentStatus}</p>
            <p><strong>Last Updated:</strong> ${new Date(applicationData.updatedAt).toLocaleString()}</p>
        </div>
        ${timeline}
    `;
}

function attachEventListeners() {
    // Attach any dynamic event listeners here
}

// ============================================================
// Utility Functions
// ============================================================

function showAlert(message, type = 'info') {
    const container = document.getElementById('app-container');
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type}`;
    alertDiv.textContent = message;
    container.prepend(alertDiv);
    setTimeout(() => alertDiv.remove(), 5000);
}

console.log('✓ Northbridge Bank Onboarding Frontend loaded');





