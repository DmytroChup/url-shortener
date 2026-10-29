export interface UrlRequest {
  url: string;
  ttlDays?: number;
}

export interface UrlResponse {
  id: number;
  originalUrl: string;
  shortCode: string;
  createdAt: string;
  expiresAt: string | null;
}

export interface UrlAnalyticsResponse {
  shortCode: string;
  totalClicks: number;
  topCountries: Record<string, number>;
  devices: Record<string, number>;
  topBrowsers: Record<string, number>;
  topOperatingSystems: Record<string, number>;
}

export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  errors?: Record<string, string>;
}

export interface ShortenedHistoryItem {
  id: string;
  originalUrl: string;
  shortCode: string;
  shortUrl: string;
  createdAt: string;
  ttlDays?: number;
}

export interface User {
  id: number;
  role: string;
  email?: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}
