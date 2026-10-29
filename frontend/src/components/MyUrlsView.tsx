import React, { useState, useEffect, useCallback } from 'react';
import {
  Link2,
  Copy,
  Check,
  ExternalLink,
  QrCode,
  BarChart2,
  Calendar,
  Clock,
  Search,
  RefreshCw,
  AlertCircle,
  PlusCircle,
  X,
} from 'lucide-react';
import { QRCodeSVG } from 'qrcode.react';
import { api, ApiError } from '../api/client';
import type { UrlResponse } from '../types';

interface MyUrlsViewProps {
  onViewAnalytics: (shortCode: string) => void;
  onNavigateToShorten: () => void;
}

export const MyUrlsView: React.FC<MyUrlsViewProps> = ({
  onViewAnalytics,
  onNavigateToShorten,
}) => {
  const [urls, setUrls] = useState<UrlResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [hasFetchedOnce, setHasFetchedOnce] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [copiedCode, setCopiedCode] = useState<string | null>(null);
  const [activeQrModalCode, setActiveQrModalCode] = useState<string | null>(null);

  const [currentTime] = useState(() => Date.now());

  const fetchUrls = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await api.getMyUrls();
      setUrls(data);
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError('Failed to load your shortened URLs. Please try again.');
      }
    } finally {
      setIsLoading(false);
      setHasFetchedOnce(true);
    }
  }, []);

  useEffect(() => {
    fetchUrls();
  }, [fetchUrls]);

  const handleCopy = async (shortCode: string) => {
    const fullUrl = `${window.location.origin}/${shortCode}`;
    try {
      await navigator.clipboard.writeText(fullUrl);
      setCopiedCode(shortCode);
      setTimeout(() => setCopiedCode(null), 2000);
    } catch {
      // fallback
    }
  };

  const filteredUrls = urls.filter((item) => {
    const q = searchQuery.toLowerCase().trim();
    if (!q) return true;
    return (
      item.shortCode.toLowerCase().includes(q) ||
      item.originalUrl.toLowerCase().includes(q)
    );
  });

  const formatDate = (isoString: string) => {
    try {
      const d = new Date(isoString);
      return d.toLocaleDateString(undefined, {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
      });
    } catch {
      return isoString;
    }
  };

  const getExpirationStatus = (expiresAt: string | null) => {
    if (!expiresAt) {
      return { text: 'No expiration', isExpired: false, badgeClass: 'bg-slate-800 text-slate-300' };
    }
    const exp = new Date(expiresAt).getTime();
    const diffDays = Math.ceil((exp - currentTime) / (1000 * 60 * 60 * 24));

    if (diffDays <= 0) {
      return { text: 'Expired', isExpired: true, badgeClass: 'bg-red-500/10 text-red-400 border border-red-500/20' };
    }
    return {
      text: `Expires in ${diffDays}d`,
      isExpired: false,
      badgeClass: 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20',
    };
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-200">
      {/* Top Header Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-slate-900/60 backdrop-blur-md p-5 rounded-2xl border border-slate-800">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-white tracking-tight flex items-center gap-2.5">
            <Link2 className="w-6 h-6 text-emerald-400" aria-hidden="true" />
            <span>My Shortened Links</span>
          </h1>
          <p className="text-xs sm:text-sm text-slate-400 mt-1">
            Manage your account links, monitor status, and view click analytics
          </p>
        </div>

        <div className="flex items-center gap-2 self-start sm:self-auto">
          <button
            onClick={fetchUrls}
            disabled={isLoading}
            className="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-slate-800/80 hover:bg-slate-800 text-slate-300 hover:text-white border border-slate-700/60 text-xs font-medium transition-colors cursor-pointer disabled:opacity-50"
            title="Refresh list"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin text-emerald-400' : ''}`} aria-hidden="true" />
            <span>{isLoading ? 'Updating...' : 'Refresh'}</span>
          </button>

          <button
            onClick={onNavigateToShorten}
            className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-semibold text-xs transition-colors shadow-sm shadow-emerald-500/20 cursor-pointer"
          >
            <PlusCircle className="w-3.5 h-3.5" aria-hidden="true" />
            <span>New Link</span>
          </button>
        </div>
      </div>

      {/* Search Input */}
      {urls.length > 0 && (
        <div className="relative">
          <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-500">
            <Search className="w-4 h-4" aria-hidden="true" />
          </div>
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search your links by code or destination URL..."
            className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-slate-900/80 border border-slate-800 text-white placeholder-slate-500 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500/50 focus:border-emerald-500 transition-colors"
          />
          {searchQuery && (
            <button
              onClick={() => setSearchQuery('')}
              className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-slate-400 hover:text-slate-200"
              aria-label="Clear search"
            >
              <X className="w-4 h-4" aria-hidden="true" />
            </button>
          )}
        </div>
      )}

      {/* Error Banner */}
      {error && (
        <div className="flex items-center gap-3 p-4 rounded-xl bg-red-500/10 border border-red-500/30 text-red-300 text-sm">
          <AlertCircle className="w-5 h-5 text-red-400 shrink-0" aria-hidden="true" />
          <div className="flex-1">
            <p>{error}</p>
          </div>
          <button
            onClick={fetchUrls}
            className="px-3 py-1 rounded-lg bg-red-500/20 hover:bg-red-500/30 text-red-200 text-xs font-medium cursor-pointer"
          >
            Try Again
          </button>
        </div>
      )}

      {/* Loading Skeletons (only on initial load) */}
      {!hasFetchedOnce && isLoading && urls.length === 0 && (
        <div className="space-y-3">
          {[1, 2, 3].map((i) => (
            <div
              key={i}
              className="p-5 rounded-2xl bg-slate-900/40 border border-slate-800/60 animate-pulse space-y-3"
            >
              <div className="flex items-center justify-between">
                <div className="w-40 h-5 bg-slate-800 rounded-lg"></div>
                <div className="w-24 h-5 bg-slate-800 rounded-lg"></div>
              </div>
              <div className="w-3/4 h-4 bg-slate-800/60 rounded-md"></div>
            </div>
          ))}
        </div>
      )}

      {/* Empty State */}
      {hasFetchedOnce && urls.length === 0 && !error && (
        <div className="p-12 text-center bg-slate-900/40 rounded-2xl border border-slate-800/80">
          <div className="w-14 h-14 mx-auto rounded-2xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400 mb-4 shadow-inner">
            <Link2 className="w-7 h-7" aria-hidden="true" />
          </div>
          <h2 className="text-lg font-bold text-white mb-1">No links created yet</h2>
          <p className="text-sm text-slate-400 max-w-sm mx-auto mb-6">
            Shorten your first link while signed in to track clicks and view analytics.
          </p>
          <button
            onClick={onNavigateToShorten}
            className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-semibold text-sm transition-all shadow-md shadow-emerald-500/20 cursor-pointer"
          >
            <PlusCircle className="w-4 h-4" aria-hidden="true" />
            <span>Create Your First Link</span>
          </button>
        </div>
      )}

      {/* No Search Matches */}
      {!isLoading && urls.length > 0 && filteredUrls.length === 0 && (
        <div className="p-8 text-center bg-slate-900/30 rounded-2xl border border-slate-800/50">
          <p className="text-slate-400 text-sm">No links matched your search "{searchQuery}"</p>
          <button
            onClick={() => setSearchQuery('')}
            className="mt-2 text-xs text-emerald-400 hover:underline font-medium cursor-pointer"
          >
            Clear search filter
          </button>
        </div>
      )}

      {/* Links List */}
      <div className="space-y-3">
        {filteredUrls.map((item) => {
          const fullShortUrl = `${window.location.origin}/${item.shortCode}`;
          const expStatus = getExpirationStatus(item.expiresAt);
          const isCopied = copiedCode === item.shortCode;

          return (
            <div
              key={item.id}
              className="p-4 sm:p-5 rounded-2xl bg-slate-900/80 hover:bg-slate-900 border border-slate-800/80 hover:border-slate-700/80 transition-all duration-150 flex flex-col md:flex-row md:items-center justify-between gap-4 group"
            >
              {/* Left Info */}
              <div className="space-y-1.5 min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <a
                    href={fullShortUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="font-mono font-semibold text-sm sm:text-base text-emerald-400 hover:text-emerald-300 hover:underline flex items-center gap-1.5 truncate"
                  >
                    <span>{fullShortUrl}</span>
                    <ExternalLink className="w-3.5 h-3.5 shrink-0 opacity-70 group-hover:opacity-100" aria-hidden="true" />
                  </a>

                  <span className={`text-[10px] uppercase font-semibold px-2 py-0.5 rounded-full ${expStatus.badgeClass}`}>
                    {expStatus.text}
                  </span>
                </div>

                <div className="text-xs text-slate-400 truncate max-w-xl" title={item.originalUrl}>
                  <span className="text-slate-500 mr-1.5">Destination:</span>
                  <span className="text-slate-300 font-mono select-all">{item.originalUrl}</span>
                </div>

                <div className="flex items-center gap-4 text-[11px] text-slate-500 pt-0.5">
                  <span className="flex items-center gap-1">
                    <Calendar className="w-3 h-3 text-slate-600" aria-hidden="true" />
                    <span>Created {formatDate(item.createdAt)}</span>
                  </span>

                  {item.expiresAt && (
                    <span className="flex items-center gap-1">
                      <Clock className="w-3 h-3 text-slate-600" aria-hidden="true" />
                      <span>Expires {formatDate(item.expiresAt)}</span>
                    </span>
                  )}
                </div>
              </div>

              {/* Right Action Buttons */}
              <div className="flex items-center gap-1.5 shrink-0 self-end md:self-auto border-t md:border-t-0 pt-3 md:pt-0 border-slate-800/60 w-full md:w-auto justify-end">
                <button
                  onClick={() => handleCopy(item.shortCode)}
                  className={`flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-medium transition-colors cursor-pointer border ${
                    isCopied
                      ? 'bg-emerald-500/20 text-emerald-300 border-emerald-500/40'
                      : 'bg-slate-800 text-slate-300 hover:text-white hover:bg-slate-700/80 border-slate-700/60'
                  }`}
                  aria-label="Copy short link"
                >
                  {isCopied ? (
                    <>
                      <Check className="w-3.5 h-3.5 text-emerald-400" aria-hidden="true" />
                      <span>Copied!</span>
                    </>
                  ) : (
                    <>
                      <Copy className="w-3.5 h-3.5" aria-hidden="true" />
                      <span>Copy</span>
                    </>
                  )}
                </button>

                <button
                  onClick={() => setActiveQrModalCode(item.shortCode)}
                  className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700/80 text-slate-300 hover:text-white border border-slate-700/60 transition-colors cursor-pointer"
                  title="View QR Code"
                  aria-label="Show QR code"
                >
                  <QrCode className="w-4 h-4" aria-hidden="true" />
                </button>

                <button
                  onClick={() => onViewAnalytics(item.shortCode)}
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 text-xs font-semibold transition-colors cursor-pointer"
                  title="View analytics for this link"
                >
                  <BarChart2 className="w-3.5 h-3.5" aria-hidden="true" />
                  <span>Analytics</span>
                </button>
              </div>
            </div>
          );
        })}
      </div>

      {/* QR Code Modal */}
      {activeQrModalCode && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-md animate-in fade-in duration-200"
          onClick={() => setActiveQrModalCode(null)}
          role="dialog"
          aria-modal="true"
        >
          <div
            className="relative bg-slate-900 border border-slate-800 rounded-2xl p-6 max-w-xs w-full text-center shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <button
              onClick={() => setActiveQrModalCode(null)}
              className="absolute top-4 right-4 p-1 rounded-lg text-slate-400 hover:text-slate-100 hover:bg-slate-800"
              aria-label="Close QR dialog"
            >
              <X className="w-5 h-5" aria-hidden="true" />
            </button>

            <h3 className="text-base font-bold text-white mb-1">QR Code</h3>
            <p className="text-xs text-slate-400 mb-4 font-mono truncate">
              {window.location.origin}/{activeQrModalCode}
            </p>

            <div className="bg-white p-4 rounded-xl inline-block shadow-md">
              <QRCodeSVG
                value={`${window.location.origin}/${activeQrModalCode}`}
                size={180}
                level="M"
              />
            </div>

            <div className="mt-5">
              <button
                onClick={() => handleCopy(activeQrModalCode)}
                className="w-full py-2 px-3 rounded-xl bg-slate-800 hover:bg-slate-700 text-xs text-slate-200 font-medium transition-colors"
              >
                Copy Link
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
