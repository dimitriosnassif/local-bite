import axios from 'axios';

// Base API configuration
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add auth token to requests if available
api.interceptors.request.use((config) => {
  const token = typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Handle auth errors globally
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Clear tokens on auth failure
      if (typeof window !== 'undefined') {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
      }
    }
    return Promise.reject(error);
  }
);

// Auth API Types
export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phoneNumber?: string;
  role?: 'BUYER' | 'SELLER';
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string | null;
  refreshToken: string | null;
  tokenType: string;
  expiresIn: number | null;
  user: UserInfo | null;
}

export interface UserInfo {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber?: string;
  emailVerified: boolean;
  provider: string;
  roles: string[];
}

// Authentication API calls
export const authApi = {
  // Register new user
  register: async (data: RegisterRequest): Promise<AuthResponse> => {
    const response = await api.post('/api/auth/register', data);
    return response.data;
  },

  // Login user
  login: async (data: LoginRequest): Promise<AuthResponse> => {
    const response = await api.post('/api/auth/login', data);
    return response.data;
  },

  // Verify email
  verifyEmail: async (token: string): Promise<{ success: boolean; message: string }> => {
    const response = await api.post(`/api/auth/verify-email?token=${token}`);
    return response.data;
  },

  // Manually verify email (for testing)
  manualVerify: async (email: string): Promise<{ success: boolean; message: string }> => {
    const response = await api.post('/api/auth/manual-verify', { email });
    return response.data;
  },

  // Check verification status
  checkVerificationStatus: async (email: string): Promise<{ verified: boolean; email: string }> => {
    const response = await api.get(`/api/auth/verification-status?email=${email}`);
    return response.data;
  },

  // Resend verification email
  resendVerification: async (email: string): Promise<{ success: boolean; message: string }> => {
    const response = await api.post('/api/auth/resend-verification', { email });
    return response.data;
  },

  // Get current user info
  getCurrentUser: async (): Promise<any> => {
    const response = await api.get('/api/auth/me');
    return response.data;
  },

  // Get user profile (alias for getCurrentUser for OAuth flow)
  getProfile: async (): Promise<UserInfo> => {
    const response = await api.get('/api/auth/me');
    return response.data;
  },

  // Health check
  healthCheck: async (): Promise<any> => {
    const response = await api.get('/api/auth/health');
    return response.data;
  },
}; 