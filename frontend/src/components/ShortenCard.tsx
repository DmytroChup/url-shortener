import React, { useState } from 'react';
import {
  Link2,
  ArrowRight,
  Copy,
  Check,
  ExternalLink,
  QrCode,
  Clock,
  AlertCircle,
  Sparkles,
  BarChart2,
  RotateCcw,
  Layers,
  ChevronDown,
  ChevronUp
} from 'lucide-react';
import { QRCodeSVG } from 'qrcode.react';
import { api, ApiError } from '../api/client';
import type { ShortenedHistoryItem } from '../types';

interface ShortenCardProps {
  onViewAnalytics: (shortCode: string) => void;
  history: ShortenedHistoryItem[];
  onAddHistory: (item: ShortenedHistoryItem) => void;
  onClearHistory: () => void;
}

const TTL_OPTIONS = [
  { label: '7 days', value: 7 },
  { label: '14 days', value: 14 },
  { label: '30 days (default)', value: 30 },
  { label: '90 days', value: 90 },
  { label: '365 days (1 year)', value: 365 },
];

export const ShortenCard: React.FC<ShortenCardProps> = ({
  onViewAnalytics,
  history,
  onAddHistory,
  onClearHistory,
}) => {
  const [url, setUrl] = useState('');
  const [ttlDays, setTtlDays] = useState<number>(30);
  const [showAdvanced, setShowAdvanced] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [currentResult, setCurrentResult] = useState<ShortenedHistoryItem | null>(null);
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [showQr, setShowQr] = useState(false);

  // Form URL validation helper
  const isValidUrl = (value: string): boolean => {
    try {
      const parsed = new URL(value);
      return parsed.protocol === 'http:' || parsed.protocol === 'https:';
    } catch {
      return false;
    }
  };

  const handlePaste = async () => {
    try {
      const text = await navigator.clipboard.readText();
      if (text) {
        setUrl(text.trim());
        setError(null);
      }
    } catch {
      // Clipboard access denied or unsupported
    }
  };

  const handleSubmit = async (e: React.SubmitEvent) => {
    e.preventDefault();
    const trimmed = url.trim();

    if (!trimmed) {
      setError('Please enter a destination URL');
      return;
    }

    // Auto prepend https if user typed example.com
    let targetUrl = trimmed;
    if (!/^https?:\/\//i.test(targetUrl)) {
      targetUrl = `https://${targetUrl}`;
    }

    if (!isValidUrl(targetUrl)) {
      setError('Please enter a valid URL including a valid domain (e.g. https://example.com)');
      return;
    }

    setError(null);
    setIsLoading(true);

    try {
      const shortCode = await api.shortenUrl({
        url: targetUrl,
        ttlDays: ttlDays || undefined,
      });

      const shortUrl = `${window.location.origin}/${shortCode}`;

      const historyItem: ShortenedHistoryItem = {
        id: crypto.randomUUID(),
        originalUrl: targetUrl,
        shortCode,
        shortUrl,
        createdAt: new Date().toISOString(),
        ttlDays,
      };

      setCurrentResult(historyItem);
      onAddHistory(historyItem);
      setUrl('');
      setShowQr(false);
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Failed to shorten URL. Make sure the API Gateway is running.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleCopy = async (text: string, id: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopiedId(id);
      setTimeout(() => setCopiedId(null), 2000);
    } catch {
      // Fallback
    }
  };

  return (
    <div className="space-y-8">
      {/* Hero Header */}
      <div className="text-center space-y-3 pt-6 sm:pt-10">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-slate-900 border border-slate-800 text-xs text-slate-300">
          <Sparkles className="w-3.5 h-3.5 text-emerald-400" />
          <span>Fast, Secure & Free URL Shortener</span>
        </div>
        <h1 className="text-3xl sm:text-5xl font-extrabold text-white tracking-tight">
          Shorten Links with <span className="bg-linear-to-r from-emerald-400 to-teal-300 bg-clip-text text-transparent">Speed & Simplicity</span>
        </h1>
        <p className="text-slate-400 max-w-xl mx-auto text-sm sm:text-base">
          Create clean, memorable links in seconds with real-time click analytics and instant redirects.
        </p>
      </div>

      {/* Main Input Form Card */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-5 sm:p-7 shadow-2xl backdrop-blur-xl transition-all">
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <label htmlFor="url-input" className="block text-xs font-semibold uppercase tracking-wider text-slate-400">
              Destination URL
            </label>
            <div className="relative flex items-center">
              <div className="absolute left-4 pointer-events-none text-slate-500">
                <Link2 className="w-5 h-5 text-emerald-400/80" />
              </div>
              <input
                id="url-input"
                type="text"
                value={url}
                onChange={(e) => {
                  setUrl(e.target.value);
                  if (error) setError(null);
                }}
                placeholder="https://your-super-long-link-here.com/resource/details..."
                className="w-full pl-12 pr-24 py-3.5 bg-slate-950 border border-slate-700/80 rounded-xl text-slate-100 placeholder:text-slate-600 focus:outline-none focus:ring-2 focus:ring-emerald-500/50 focus:border-emerald-500 text-sm sm:text-base transition-all font-mono"
                autoComplete="off"
                autoFocus
              />
              <button
                type="button"
                onClick={handlePaste}
                className="absolute right-3 px-2.5 py-1 text-xs font-medium text-slate-400 hover:text-white bg-slate-800/80 hover:bg-slate-700 rounded-md transition-all cursor-pointer border border-slate-700/50"
              >
                Paste
              </button>
            </div>
          </div>

          {/* Inline Validation Error */}
          {error && (
            <div
              role="alert"
              className="flex items-center gap-2 p-3 rounded-xl bg-red-500/10 border border-red-500/20 text-red-400 text-xs sm:text-sm animate-in fade-in duration-200"
            >
              <AlertCircle className="w-4 h-4 shrink-0 text-red-400" />
              <span>{error}</span>
            </div>
          )}

          {/* Advanced Options Accordion */}
          <div className="pt-1">
            <button
              type="button"
              onClick={() => setShowAdvanced(!showAdvanced)}
              className="inline-flex items-center gap-1.5 text-xs text-slate-400 hover:text-slate-200 transition-colors cursor-pointer"
            >
              <Clock className="w-3.5 h-3.5 text-slate-500" />
              <span>Expiration Settings (TTL)</span>
              {showAdvanced ? (
                <ChevronUp className="w-3.5 h-3.5" />
              ) : (
                <ChevronDown className="w-3.5 h-3.5" />
              )}
            </button>

            {showAdvanced && (
              <div className="mt-3 p-4 rounded-xl bg-slate-950/70 border border-slate-800 space-y-2.5 animate-in fade-in duration-150">
                <div className="flex items-center justify-between text-xs">
                  <span className="text-slate-300 font-medium">Link Retention Period</span>
                  <span className="text-emerald-400 font-mono">{ttlDays} days</span>
                </div>
                <div className="grid grid-cols-2 sm:grid-cols-5 gap-2">
                  {TTL_OPTIONS.map((opt) => (
                    <button
                      key={opt.value}
                      type="button"
                      onClick={() => setTtlDays(opt.value)}
                      className={`px-3 py-2 rounded-lg text-xs font-medium text-center transition-all cursor-pointer border ${
                        ttlDays === opt.value
                          ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/40 shadow-sm'
                          : 'bg-slate-900 text-slate-400 border-slate-800 hover:bg-slate-850 hover:text-slate-200'
                      }`}
                    >
                      {opt.label}
                    </button>
                  ))}
                </div>
                <p className="text-[11px] text-slate-500">
                  After expiration, the link returns 410 Gone and is safely purged by background schedulers.
                </p>
              </div>
            )}
          </div>

          {/* Submit Button */}
          <button
            type="submit"
            disabled={isLoading}
            className="w-full mt-2 py-3.5 px-6 rounded-xl bg-linear-to-r from-emerald-500 to-teal-500 hover:from-emerald-400 hover:to-teal-400 text-slate-950 font-semibold text-sm sm:text-base flex items-center justify-center gap-2 shadow-lg shadow-emerald-500/20 active:scale-[0.99] transition-all cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isLoading ? (
              <>
                <RotateCcw className="w-4 h-4 animate-spin text-slate-950" />
                <span>Generating Base62 Short Code...</span>
              </>
            ) : (
              <>
                <span>Shorten URL</span>
                <ArrowRight className="w-4 h-4 text-slate-950" />
              </>
            )}
          </button>
        </form>

        {/* Current Result Card */}
        {currentResult && (
          <div className="mt-6 pt-6 border-t border-slate-800 animate-in fade-in slide-in-from-top-2 duration-200">
            <div className="p-4 sm:p-5 rounded-xl bg-slate-950 border border-emerald-500/30 shadow-inner space-y-4">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-emerald-400 uppercase tracking-wider flex items-center gap-1.5">
                  <Sparkles className="w-3.5 h-3.5" />
                  Link Shortened Successfully
                </span>
                <span className="text-xs text-slate-500 font-mono">
                  TTL: {currentResult.ttlDays || 30}d
                </span>
              </div>

              {/* Generated URL Display & Actions */}
              <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 p-3 bg-slate-900 rounded-lg border border-slate-800">
                <div className="truncate font-mono text-emerald-300 font-semibold text-base sm:text-lg select-all">
                  {currentResult.shortUrl}
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <button
                    type="button"
                    onClick={() => handleCopy(currentResult.shortUrl, 'current')}
                    className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-medium transition-all cursor-pointer ${
                      copiedId === 'current'
                        ? 'bg-emerald-500 text-slate-950'
                        : 'bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                    }`}
                  >
                    {copiedId === 'current' ? (
                      <>
                        <Check className="w-3.5 h-3.5" />
                        <span>Copied!</span>
                      </>
                    ) : (
                      <>
                        <Copy className="w-3.5 h-3.5" />
                        <span>Copy</span>
                      </>
                    )}
                  </button>

                  <a
                    href={currentResult.shortUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="p-1.5 rounded-md text-slate-400 hover:text-white bg-slate-800 hover:bg-slate-700 transition-all cursor-pointer border border-slate-700/60"
                    title="Open short link in new tab"
                  >
                    <ExternalLink className="w-4 h-4" />
                  </a>

                  <button
                    type="button"
                    onClick={() => setShowQr(!showQr)}
                    className={`p-1.5 rounded-md transition-all cursor-pointer border ${
                      showQr
                        ? 'bg-emerald-500 text-slate-950 border-emerald-400'
                        : 'text-slate-400 hover:text-white bg-slate-800 hover:bg-slate-700 border-slate-700/60'
                    }`}
                    title="Toggle QR Code"
                  >
                    <QrCode className="w-4 h-4" />
                  </button>

                  <button
                    type="button"
                    onClick={() => onViewAnalytics(currentResult.shortCode)}
                    className="flex items-center gap-1 px-2.5 py-1.5 rounded-md text-xs font-medium text-slate-300 hover:text-white bg-slate-800 hover:bg-slate-700 border border-slate-700/60 cursor-pointer"
                    title="View Click Analytics"
                  >
                    <BarChart2 className="w-3.5 h-3.5 text-teal-400" />
                    <span className="hidden sm:inline">Stats</span>
                  </button>
                </div>
              </div>

              {/* QR Code Container */}
              {showQr && (
                <div className="flex flex-col items-center justify-center p-4 bg-white rounded-xl max-w-xs mx-auto animate-in zoom-in-95 duration-150">
                  <QRCodeSVG value={currentResult.shortUrl} size={160} level="M" />
                  <span className="mt-2 text-[11px] text-slate-700 font-mono font-medium">
                    Scan to visit {currentResult.shortCode}
                  </span>
                </div>
              )}

              {/* Original destination preview */}
              <div className="text-xs text-slate-500 truncate">
                <span className="text-slate-400 font-medium">Destination:</span>{' '}
                <span className="font-mono text-slate-400">{currentResult.originalUrl}</span>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Recent Links History List */}
      {history.length > 0 && (
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-semibold text-slate-300 uppercase tracking-wider flex items-center gap-2">
              <Layers className="w-4 h-4 text-emerald-400" />
              <span>Recent Links ({history.length})</span>
            </h2>
            <button
              onClick={onClearHistory}
              className="text-xs text-slate-500 hover:text-slate-300 transition-colors cursor-pointer"
            >
              Clear History
            </button>
          </div>

          <div className="divide-y divide-slate-800/80 rounded-xl bg-slate-900/60 border border-slate-800 overflow-hidden">
            {history.slice(0, 5).map((item) => (
              <div
                key={item.id}
                className="p-3.5 sm:p-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2.5 hover:bg-slate-850/50 transition-colors"
              >
                <div className="space-y-1 min-w-0 max-w-full sm:max-w-[65%]">
                  <div className="flex items-center gap-2">
                    <span className="font-mono font-semibold text-sm text-emerald-400">
                      {item.shortCode}
                    </span>
                    <span className="text-xs text-slate-500 font-mono">
                      {new Date(item.createdAt).toLocaleDateString()}
                    </span>
                  </div>
                  <p className="text-xs text-slate-400 truncate font-mono" title={item.originalUrl}>
                    {item.originalUrl}
                  </p>
                </div>

                <div className="flex items-center gap-2 self-end sm:self-center shrink-0">
                  <button
                    onClick={() => handleCopy(item.shortUrl, item.id)}
                    className="p-1.5 rounded-lg text-slate-400 hover:text-white bg-slate-800 hover:bg-slate-700 transition-colors cursor-pointer"
                    title="Copy short link"
                  >
                    {copiedId === item.id ? (
                      <Check className="w-4 h-4 text-emerald-400" />
                    ) : (
                      <Copy className="w-4 h-4" />
                    )}
                  </button>

                  <a
                    href={item.shortUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="p-1.5 rounded-lg text-slate-400 hover:text-white bg-slate-800 hover:bg-slate-700 transition-colors cursor-pointer"
                    title="Open link"
                  >
                    <ExternalLink className="w-4 h-4" />
                  </a>

                  <button
                    onClick={() => onViewAnalytics(item.shortCode)}
                    className="flex items-center gap-1 px-2.5 py-1.5 rounded-lg text-xs font-medium text-slate-300 hover:text-white bg-slate-800 hover:bg-slate-700 transition-colors cursor-pointer"
                  >
                    <BarChart2 className="w-3.5 h-3.5 text-teal-400" />
                    <span>Analytics</span>
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
