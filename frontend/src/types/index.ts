export interface UrlRequest {
  url: string;
  ttlDays?: number;
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
