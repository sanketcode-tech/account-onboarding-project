/* Auth helper: store token in sessionStorage, validate and logout helpers */
(function(window){
    const TOKEN_KEY = 'jwtToken';
    const DEFAULT_AUTH_BASE = (window.API_BASE && window.API_BASE.auth) ? window.API_BASE.auth : 'http://localhost:8081/api/auth';

    function setToken(token) {
        if (token) sessionStorage.setItem(TOKEN_KEY, token);
    }

    function getToken() {
        return sessionStorage.getItem(TOKEN_KEY);
    }

    function clearToken() {
        sessionStorage.removeItem(TOKEN_KEY);
    }

    function isLoggedIn() {
        return !!getToken();
    }

    async function validateToken() {
        const token = getToken();
        if (!token) return false;
        try {
            const res = await window.apiClient.fetch((window.API_BASE && window.API_BASE.auth ? window.API_BASE.auth : DEFAULT_AUTH_BASE) + '/validate', { method: 'GET' });
            return res.ok;
        } catch (e) {
            return false;
        }
    }

    function logout(redirectToLogin = true) {
        clearToken();
        if (redirectToLogin) window.location.href = 'index.html';
    }

    window.auth = {
        setToken, getToken, clearToken, isLoggedIn, validateToken, logout
    };
})(window);
