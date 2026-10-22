import type { UrlRequest, UrlAnalyticsResponse, ProblemDetail } from '../types';

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
    }

    throw new ApiError(errorMessage, response.status, errorDetail);
  }

  const contentType = response.headers.get('content-type');
  if (contentType && contentType.includes('application/json')) {
    return response.json();
  }

  // Handle plain text response (like shortCode string from /shorten)
  const text = await response.text();
  return text as unknown as T;
}

export const api = {
  /**
   * Shorten a long URL
   * POST /api/v1/urls/shorten
   */
  async shortenUrl(payload: UrlRequest): Promise<string> {
    const response = await fetch('/api/v1/urls/shorten', {
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
    const response = await fetch(`/api/v1/analytics/${encodeURIComponent(shortCode)}`, {
      method: 'GET',
      headers: {
        'Accept': 'application/json',
      },
    });

    return handleResponse<UrlAnalyticsResponse>(response);
  },
};
