import axios from 'axios';

const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8082/api',
    headers: {
        'Content-Type': 'application/json',
    }
});

// We can attach a token here if we add login later, but for now caseId is the main identifier.
// The backend requires an email to create a case, so let's mock a simple login/register wrapper if needed.
// Actually, since there's no frontend auth yet, we'll just register a dummy user on startup or rely on the backend token.

// Since the backend uses JWT authentication (SecurityConfig), we need a way to get a token.
// The user hasn't specified a login screen. The backend has AuthController? 
// Let's check if there's an Auth endpoint in the backend. 
// I'll leave the token injection ready.
api.interceptors.request.use((config) => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response && (error.response.status === 401 || error.response.status === 403)) {
            localStorage.removeItem('token');
            window.location.href = '/';
        }
        return Promise.reject(error);
    }
);

export default api;
