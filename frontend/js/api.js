// Central API client wrapper — attaches JWT and handles 401/403 by redirecting to login
(function(window){
    const DEFAULTS = {
        auth: 'http://localhost:8081/api/auth',
        application: 'http://localhost:8082/api/v1/applications',
        offers: 'http://localhost:8082/api/offers',
        documents: 'http://localhost:8084/api/documents',
        accounts: 'http://localhost:8083/api/accounts',
    };

    function getToken() {
        return sessionStorage.getItem('jwtToken');
    }

    async function apiFetch(url, opts = {}) {
        opts.headers = opts.headers || {};
        const token = getToken();
        if (token) {
            opts.headers['Authorization'] = `Bearer ${token}`;
        }

        // Ensure defaults
        opts.method = opts.method || 'GET';

        const res = await fetch(url, opts);
        if (res.status === 401 || res.status === 403) {
            // centrally handle auth failures
            sessionStorage.removeItem('jwtToken');
            // redirect to login page
            window.location.href = 'index.html';
            // throw so callers do not continue
            throw new Error('Unauthorized');
        }
        return res;
    }

    function apiGet(url) { return apiFetch(url, { method: 'GET' }); }
    function apiPost(url, body) { return apiFetch(url, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) }); }
    function apiPut(url, body) { return apiFetch(url, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) }); }
    function apiPostForm(url, formData) { return apiFetch(url, { method: 'POST', body: formData }); }

    window.apiClient = {
        defaults: DEFAULTS,
        getToken,
        fetch: apiFetch,
        get: apiGet,
        post: apiPost,
        put: apiPut,
        postForm: apiPostForm,
    };

})(window);
