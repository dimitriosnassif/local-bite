'use client';

import React, { createContext, useContext, useEffect, useState } from 'react';
import { authApi, UserInfo, LoginRequest, RegisterRequest } from '@/lib/api';

interface AuthContextType {
  user: UserInfo | null;
  loading: boolean;
  login: (data: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  loginWithToken: (tokenData: { token: string; refreshToken: string; tokenType: string; expiresIn: number }) => Promise<void>;
  logout: () => void;
  manualVerify: (email: string) => Promise<void>;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserInfo | null>(null);
  const [loading, setLoading] = useState(true);

  const isAuthenticated = !!user;

  // Check for existing token on mount
  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      // In a real app, you'd validate the token here
      // For now, we'll just check if we have user data
      const userData = localStorage.getItem('userData');
      if (userData) {
        try {
          setUser(JSON.parse(userData));
        } catch (error) {
          console.error('Error parsing user data:', error);
          localStorage.removeItem('userData');
          localStorage.removeItem('accessToken');
          localStorage.removeItem('refreshToken');
        }
      }
    }
    setLoading(false);
  }, []);

  const login = async (data: LoginRequest) => {
    try {
      const response = await authApi.login(data);
      
      if (response.token && response.user) {
        // Store tokens and user data
        localStorage.setItem('accessToken', response.token);
        if (response.refreshToken) {
          localStorage.setItem('refreshToken', response.refreshToken);
        }
        localStorage.setItem('userData', JSON.stringify(response.user));
        setUser(response.user);
      } else {
        throw new Error('Login failed - no token received');
      }
    } catch (error: any) {
      console.error('Login error:', error);
      throw new Error(error.response?.data?.message || 'Login failed');
    }
  };

  const register = async (data: RegisterRequest) => {
    try {
      const response = await authApi.register(data);
      // Registration successful but requires email verification
      // Don't log in automatically - user needs to verify email first
      console.log('Registration successful:', response);
    } catch (error: any) {
      console.error('Registration error:', error);
      throw new Error(error.response?.data?.message || 'Registration failed');
    }
  };

  const loginWithToken = async (tokenData: { token: string; refreshToken: string; tokenType: string; expiresIn: number }) => {
    try {
      // Store tokens
      localStorage.setItem('accessToken', tokenData.token);
      localStorage.setItem('refreshToken', tokenData.refreshToken);
      
      // Get user profile using the token
      const userProfile = await authApi.getProfile();
      localStorage.setItem('userData', JSON.stringify(userProfile));
      setUser(userProfile);
    } catch (error: any) {
      console.error('Token login error:', error);
      // Clean up on error
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('userData');
      throw new Error('Failed to authenticate with provided token');
    }
  };

  const manualVerify = async (email: string) => {
    try {
      await authApi.manualVerify(email);
    } catch (error: any) {
      console.error('Manual verification error:', error);
      throw new Error(error.response?.data?.message || 'Verification failed');
    }
  };

  const logout = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userData');
    setUser(null);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        login,
        register,
        loginWithToken,
        logout,
        manualVerify,
        isAuthenticated,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
} 