import type {
  UrlRequest,
  UrlResponse,
  UrlAnalyticsResponse,
  ProblemDetail,
  AuthResponse,
} from '../types';

export class ApiError extends Error {
  status: number;
  problem?: ProblemDetail;

  constructor(message: string, status: number, problem?: ProblemDetail) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.problem = problem;
  }
}

const STORAGE_REFRESH_KEY = 'url_shortener_refresh_token';
const STORAGE_ACCESS_KEY = 'url_shortener_access_token';

let currentAccessToken: string | null = null;
try {
  currentAccessToken = localStorage.getItem(STORAGE_ACCESS_KEY);
} catch {
  // localStorage may be disabled
}

let onUnauthorizedCallback: (() => void) | null = null;
let refreshPromise: Promise<AuthResponse> | null = null;

export const tokenStorage = {
  getAccessToken(): string | null {
    return currentAccessToken;
  },
  setAccessToken(token: string | null) {
    currentAccessToken = token;
    try {
      if (token) {
        localStorage.setItem(STORAGE_ACCESS_KEY, token);
      } else {
        localStorage.removeItem(STORAGE_ACCESS_KEY);
      }
    } catch {
      // ignore
    }
  },
  getRefreshToken(): string | null {
    try {
      return localStorage.getItem(STORAGE_REFRESH_KEY);
    } catch {
      return null;
    }
  },
  setRefreshToken(token: string | null) {
    try {
      if (token) {
        localStorage.setItem(STORAGE_REFRESH_KEY, token);
      } else {
        localStorage.removeItem(STORAGE_REFRESH_KEY);
      }
    } catch {
      // ignore
    }
  },
  clear() {
    this.setAccessToken(null);
    this.setRefreshToken(null);
  },
  setOnUnauthorized(cb: () => void) {
    onUnauthorizedCallback = cb;
  },
};

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    let errorDetail: ProblemDetail | undefined;
    let errorMessage = `Request failed with status ${response.status}`;

    try {
      const text = await response.text();
      try {
        errorDetail = JSON.parse(text);
        if (errorDetail?.detail) {
          errorMessage = errorDetail.detail;
        } else if (errorDetail?.title) {
          errorMessage = errorDetail.title;
        }
      } catch {
        if (text) {
          errorMessage = text;
        }
      }
    } catch {
      // fallback
    }

    if (response.status === 429) {
      errorMessage = 'Too many requests. Rate limit exceeded, please wait a moment.';
    } else if (response.status === 404) {
      errorMessage = 'Resource not found. The short link might have expired or does not exist.';
    } else if (response.status === 403) {
      errorMessage = errorDetail?.detail || 'Access denied. You do not have permission to view this resource.';
    } else if (response.status === 401) {
      errorMessage = errorDetail?.detail || 'Unauthorized. Please sign in to continue.';
    }

    throw new ApiError(errorMessage, response.status, errorDetail);
  }

  if (response.status === 204) {
    return undefined as unknown as T;
  }

  const contentType = response.headers.get('content-type');
  if (contentType && contentType.includes('application/json')) {
    return response.json();
  }

  // Handle plain text response (like shortCode string from /shorten)
  const text = await response.text();
  return text as unknown as T;
}

/**
 * Performs a fetch with automatic Bearer token insertion and 401 Silent Refresh.
 */
async function fetchWithAuth(url: string, init: RequestInit = {}): Promise<Response> {
  const headers = new Headers(init.headers || {});

  const token = tokenStorage.getAccessToken();
  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  let response = await fetch(url, { ...init, headers });

  // If 401 Unauthorized, attempt silent refresh once
  if (response.status === 401 && !url.includes('/api/v1/auth/')) {
    const refreshToken = tokenStorage.getRefreshToken();
    if (refreshToken) {
      try {
        // Singleton promise to prevent multiple parallel refresh requests (and prevent token reuse trigger)
        if (!refreshPromise) {
          refreshPromise = (async () => {
            const refreshRes = await fetch('/api/v1/auth/refresh', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({ refreshToken }),
            });

            if (!refreshRes.ok) {
              throw new Error('Refresh token invalid or expired');
            }

            const data: AuthResponse = await refreshRes.json();
            tokenStorage.setAccessToken(data.accessToken);
            tokenStorage.setRefreshToken(data.refreshToken);
            return data;
          })().finally(() => {
            refreshPromise = null;
          });
        }

        const newAuth = await refreshPromise;

        // Retry the original request with the new access token
        const retryHeaders = new Headers(init.headers || {});
        retryHeaders.set('Authorization', `Bearer ${newAuth.accessToken}`);
        response = await fetch(url, { ...init, headers: retryHeaders });
      } catch {
        tokenStorage.clear();
        if (onUnauthorizedCallback) {
          onUnauthorizedCallback();
        }
        throw new ApiError('Session expired. Please sign in again.', 401);
      }
    } else {
      tokenStorage.clear();
      if (onUnauthorizedCallback) {
        onUnauthorizedCallback();
      }
    }
  }

  return response;
}

export const api = {
  /**
   * Shorten a long URL (works for guests or authenticated users)
   * POST /api/v1/urls/shorten
   */
  async shortenUrl(payload: UrlRequest): Promise<string> {
    const response = await fetchWithAuth('/api/v1/urls/shorten', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    });

    return handleResponse<string>(response);
  },

  /**
   * Fetch click analytics for a short code
   * GET /api/v1/analytics/{shortCode}
   */
  async getAnalytics(shortCode: string): Promise<UrlAnalyticsResponse> {
    const response = await fetchWithAuth(`/api/v1/analytics/${encodeURIComponent(shortCode)}`, {
      method: 'GET',
      headers: {
        Accept: 'application/json',
      },
    });

    return handleResponse<UrlAnalyticsResponse>(response);
  },

  /**
   * Get all links belonging to the authenticated user
   * GET /api/v1/urls/me
   */
  async getMyUrls(): Promise<UrlResponse[]> {
    const response = await fetchWithAuth('/api/v1/urls/me', {
      method: 'GET',
      headers: {
        Accept: 'application/json',
      },
    });

    return handleResponse<UrlResponse[]>(response);
  },

  /**
   * Register a new user
   * POST /api/v1/auth/register
   */
  async register(payload: { email: string; password: string }): Promise<AuthResponse> {
    const response = await fetch('/api/v1/auth/register', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    });

    return handleResponse<AuthResponse>(response);
  },

  /**
   * Login with email and password
   * POST /api/v1/auth/login
   */
  async login(payload: { email: string; password: string }): Promise<AuthResponse> {
    const response = await fetch('/api/v1/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    });

    return handleResponse<AuthResponse>(response);
  },

  /**
   * Rotate and refresh access token
   * POST /api/v1/auth/refresh
   */
  async refresh(refreshToken: string): Promise<AuthResponse> {
    const response = await fetch('/api/v1/auth/refresh', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ refreshToken }),
    });

    return handleResponse<AuthResponse>(response);
  },

  /**
   * Invalidate session / logout
   * POST /api/v1/auth/logout
   */
  async logout(refreshToken: string): Promise<void> {
    const response = await fetch('/api/v1/auth/logout', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ refreshToken }),
    });

    return handleResponse<void>(response);
  },
};
