import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { api, tokenStorage } from '../api/client';
import type { User, AuthResponse } from '../types';

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

function parseJwt(token: string): { sub?: string; role?: string; exp?: number } | null {
  try {
    const base64Url = token.split('.')[1];
    if (!base64Url) return null;
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch {
    return null;
  }
}

function extractUserFromToken(token: string, fallbackEmail?: string): User | null {
  const claims = parseJwt(token);
  if (!claims || !claims.sub) return null;

  return {
    id: Number(claims.sub),
    role: claims.role || 'ROLE_USER',
    email: fallbackEmail,
  };
}

const STORAGE_EMAIL_KEY = 'url_shortener_user_email';

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const handleAuthSuccess = useCallback((authData: AuthResponse, email: string) => {
    tokenStorage.setAccessToken(authData.accessToken);
    tokenStorage.setRefreshToken(authData.refreshToken);
    try {
      localStorage.setItem(STORAGE_EMAIL_KEY, email);
    } catch {
      // ignore
    }
    const extractedUser = extractUserFromToken(authData.accessToken, email);
    setUser(extractedUser);
  }, []);

  const logout = useCallback(async () => {
    const refreshToken = tokenStorage.getRefreshToken();
    tokenStorage.clear();
    try {
      localStorage.removeItem(STORAGE_EMAIL_KEY);
    } catch {
      // ignore
    }
    setUser(null);

    if (refreshToken) {
      try {
        await api.logout(refreshToken);
      } catch {
        // Logout should succeed locally even if network/backend fails
      }
    }
  }, []);

  useEffect(() => {
    tokenStorage.setOnUnauthorized(() => {
      setUser(null);
      try {
        localStorage.removeItem(STORAGE_EMAIL_KEY);
      } catch {
        // ignore
      }
    });

    const initAuth = async () => {
      const savedEmail = (() => {
        try {
          return localStorage.getItem(STORAGE_EMAIL_KEY) || undefined;
        } catch {
          return undefined;
        }
      })();

      const accessToken = tokenStorage.getAccessToken();
      const refreshToken = tokenStorage.getRefreshToken();

      if (accessToken) {
        const claims = parseJwt(accessToken);
        const isExpired = claims?.exp ? claims.exp * 1000 < Date.now() : true;

        if (!isExpired) {
          const loadedUser = extractUserFromToken(accessToken, savedEmail);
          setUser(loadedUser);
          setIsLoading(false);
          return;
        }
      }

      // If accessToken is missing or expired, try silent refresh
      if (refreshToken) {
        try {
          const authData = await api.refresh(refreshToken);
          handleAuthSuccess(authData, savedEmail || '');
        } catch {
          tokenStorage.clear();
          setUser(null);
        }
      }

      setIsLoading(false);
    };

    initAuth();
  }, [handleAuthSuccess]);

  const login = async (email: string, password: string) => {
    const authData = await api.login({ email, password });
    handleAuthSuccess(authData, email);
  };

  const register = async (email: string, password: string) => {
    const authData = await api.register({ email, password });
    handleAuthSuccess(authData, email);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading,
        login,
        register,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
