import React, {useState, useEffect, useCallback} from 'react';
import {
  BarChart2,
  Search,
  Globe,
  Smartphone,
  Monitor,
  Compass,
  Cpu,
  ArrowUpRight,
  AlertCircle,
  RefreshCw,
  Activity,
  MousePointerClick
} from 'lucide-react';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  Rectangle,
  PieChart,
  Pie
} from 'recharts';
import { api, ApiError } from '../api/client';
import type { UrlAnalyticsResponse } from '../types';

interface AnalyticsViewProps {
  initialCode?: string;
  onClearInitialCode?: () => void;
}

const DEVICE_COLORS = ['#10B981', '#06B6D4', '#6366F1', '#F59E0B', '#EC4899'];
const BROWSER_COLORS = ['#3B82F6', '#10B981', '#F97316', '#8B5CF6', '#EC4899'];

export const AnalyticsView: React.FC<AnalyticsViewProps> = ({
  initialCode = '',
  onClearInitialCode,
}) => {
  const [code, setCode] = useState(initialCode);
  const [analytics, setAnalytics] = useState<UrlAnalyticsResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchAnalytics = useCallback(async (shortCodeToQuery: string) => {
    const cleanCode = shortCodeToQuery.trim();
    if (!cleanCode) return;

    setIsLoading(true);
    setError(null);

    try {
      const data = await api.getAnalytics(cleanCode);
      setAnalytics(data);
    } catch (err: unknown) {
      setAnalytics(null);
      if (err instanceof ApiError) {
        setError(err.message);
      } else if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Failed to fetch analytics for this short code.');
      }
    } finally {
      setIsLoading(false);
    }
  }, []);

  // If initialCode changes, query automatically
  useEffect(() => {
    if (initialCode) {
      void fetchAnalytics(initialCode);
      onClearInitialCode?.();
    }
  }, [initialCode, fetchAnalytics, onClearInitialCode]);

  const handleSearch = (e: React.SubmitEvent) => {
    e.preventDefault();
    if (code) {
      void fetchAnalytics(code);
    }
  };

  // Helper converters for Recharts
  const mapToChartData = (mapData?: Record<string, number>) => {
    if (!mapData) return [];
    return Object.entries(mapData)
      .map(([name, value]) => ({ name: name || 'Unknown', value }))
      .sort((a, b) => b.value - a.value);
  };

  const deviceData = mapToChartData(analytics?.devices).map((d, index) => ({
    ...d,
    fill: DEVICE_COLORS[index % DEVICE_COLORS.length],
  }));
  const browserData = mapToChartData(analytics?.topBrowsers);
  const osData = mapToChartData(analytics?.topOperatingSystems);
  const countryData = mapToChartData(analytics?.topCountries);

  const totalDeviceClicks = deviceData.reduce((acc, d) => acc + d.value, 0);

  // Top summary metrics
  const topDevice = deviceData[0]?.name || 'N/A';
  const topBrowser = browserData[0]?.name || 'N/A';
  const topCountry = countryData[0]?.name || 'N/A';

  return (
    <div className="space-y-8">
      {/* Search Header */}
      <div className="text-center space-y-3 pt-6 sm:pt-10">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-slate-900 border border-slate-800 text-xs text-slate-300">
          <Activity className="w-3.5 h-3.5 text-emerald-400" />
          <span>Real-Time Click Tracking</span>
        </div>
        <h1 className="text-3xl sm:text-5xl font-extrabold text-white tracking-tight">
          Link <span className="bg-linear-to-r from-emerald-400 to-teal-300 bg-clip-text text-transparent">Analytics & Insights</span>
        </h1>
        <p className="text-slate-400 max-w-xl mx-auto text-sm sm:text-base">
          Track link performance, visitor demographics, devices, and browser distribution in real time.
        </p>
      </div>

      {/* Code Search Input Card */}
      <div className="max-w-2xl mx-auto bg-slate-900/90 border border-slate-800 rounded-2xl p-4 sm:p-6 shadow-xl backdrop-blur-md">
        <form onSubmit={handleSearch} className="flex flex-col sm:flex-row gap-3">
          <div className="relative flex-1">
            <div className="absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none text-slate-500">
              <Search className="w-4 h-4 text-emerald-400" />
            </div>
            <input
              type="text"
              value={code}
              onChange={(e) => setCode(e.target.value.trim())}
              placeholder="Enter 7-character code (e.g. aB7xK9q)"
              maxLength={16}
              className="w-full pl-10 pr-4 py-3 bg-slate-950 border border-slate-700/80 rounded-xl text-slate-100 placeholder:text-slate-600 focus:outline-none focus:ring-2 focus:ring-emerald-500/50 focus:border-emerald-500 text-sm font-mono tracking-wider sm:normal-case"
            />
          </div>
          <button
            type="submit"
            disabled={isLoading || !code}
            className="px-6 py-3 rounded-xl bg-linear-to-r from-teal-500 to-emerald-500 hover:from-teal-400 hover:to-emerald-400 text-slate-950 font-semibold text-sm flex items-center justify-center gap-2 shadow-lg shadow-teal-500/20 active:scale-[0.99] transition-all cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isLoading ? (
              <RefreshCw className="w-4 h-4 animate-spin text-slate-950" />
            ) : (
              <>
                <span>Inspect</span>
                <ArrowUpRight className="w-4 h-4 text-slate-950" />
              </>
            )}
          </button>
        </form>

        {error && (
          <div className="mt-4 flex items-center gap-2 p-3 rounded-xl bg-red-500/10 border border-red-500/20 text-red-400 text-xs sm:text-sm">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}
      </div>

      {/* Analytics Loaded Content */}
      {analytics && (
        <div className="space-y-6 animate-in fade-in duration-200">
          {/* Top KPI Cards */}
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
            {/* Total Clicks */}
            <div className="p-4 sm:p-5 rounded-2xl bg-slate-900 border border-slate-800 shadow-md flex items-center gap-4">
              <div className="p-3 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 shrink-0">
                <MousePointerClick className="w-6 h-6" />
              </div>
              <div className="min-w-0">
                <p className="text-xs font-medium uppercase tracking-wider text-slate-400">Total Clicks</p>
                <p className="text-2xl sm:text-3xl font-extrabold text-white font-mono">
                  {analytics.totalClicks.toLocaleString()}
                </p>
              </div>
            </div>

            {/* Top Device */}
            <div className="p-4 sm:p-5 rounded-2xl bg-slate-900 border border-slate-800 shadow-md flex items-center gap-4">
              <div className="p-3 rounded-xl bg-cyan-500/10 border border-cyan-500/20 text-cyan-400 shrink-0">
                <Smartphone className="w-6 h-6" />
              </div>
              <div className="min-w-0">
                <p className="text-xs font-medium uppercase tracking-wider text-slate-400">Top Device</p>
                <p className="text-lg sm:text-xl font-bold text-white truncate">
                  {topDevice}
                </p>
              </div>
            </div>

            {/* Top Browser */}
            <div className="p-4 sm:p-5 rounded-2xl bg-slate-900 border border-slate-800 shadow-md flex items-center gap-4">
              <div className="p-3 rounded-xl bg-indigo-500/10 border border-indigo-500/20 text-indigo-400 shrink-0">
                <Compass className="w-6 h-6" />
              </div>
              <div className="min-w-0">
                <p className="text-xs font-medium uppercase tracking-wider text-slate-400">Top Browser</p>
                <p className="text-lg sm:text-xl font-bold text-white truncate">
                  {topBrowser}
                </p>
              </div>
            </div>

            {/* Top Country */}
            <div className="p-4 sm:p-5 rounded-2xl bg-slate-900 border border-slate-800 shadow-md flex items-center gap-4">
              <div className="p-3 rounded-xl bg-amber-500/10 border border-amber-500/20 text-amber-400 shrink-0">
                <Globe className="w-6 h-6" />
              </div>
              <div className="min-w-0">
                <p className="text-xs font-medium uppercase tracking-wider text-slate-400">Top Country</p>
                <p className="text-lg sm:text-xl font-bold text-white truncate">
                  {topCountry}
                </p>
              </div>
            </div>
          </div>

          {/* Charts Grid */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Devices Chart */}
            <div className="p-5 sm:p-6 rounded-2xl bg-slate-900 border border-slate-800 shadow-md space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-semibold text-slate-200 uppercase tracking-wider flex items-center gap-2">
                  <Monitor className="w-4 h-4 text-emerald-400" />
                  <span>Devices Distribution</span>
                </h3>
                <span className="text-xs text-slate-500 font-mono">User-Agent parsed</span>
              </div>

              {deviceData.length > 0 ? (
                <div className="space-y-3">
                  <div className="h-52 w-full">
                    <ResponsiveContainer width="100%" height="100%">
                      <PieChart margin={{ top: 5, right: 5, bottom: 5, left: 5 }}>
                        <Pie
                          data={deviceData}
                          dataKey="value"
                          nameKey="name"
                          cx="50%"
                          cy="50%"
                          innerRadius={48}
                          outerRadius={75}
                          paddingAngle={4}
                        />
                        <Tooltip
                          contentStyle={{
                            backgroundColor: '#0F172A',
                            borderColor: '#334155',
                            borderRadius: '0.75rem',
                            color: '#F8FAFC',
                            fontSize: '12px',
                          }}
                          formatter={(val: unknown, name: unknown) => [
                            `${val} clicks`,
                            String(name)
                          ]}
                        />
                      </PieChart>
                    </ResponsiveContainer>
                  </div>

                  {/* Guaranteed descending legend (highest percentage first) */}
                  <div className="flex flex-wrap items-center justify-center gap-x-4 gap-y-2 pt-2 border-t border-slate-800/80">
                    {deviceData.map((item, index) => {
                      const percent = totalDeviceClicks > 0
                        ? Math.round((item.value / totalDeviceClicks) * 100)
                        : 0;
                      return (
                        <div key={item.name} className="flex items-center gap-1.5 text-xs">
                          <span
                            className="w-2.5 h-2.5 rounded-full shrink-0 shadow-sm"
                            style={{ backgroundColor: DEVICE_COLORS[index % DEVICE_COLORS.length] }}
                          />
                          <span className="text-slate-300 font-medium">{item.name}</span>
                          <span className="text-slate-400 font-mono font-medium">({percent}%)</span>
                        </div>
                      );
                    })}
                  </div>
                </div>
              ) : (
                <p className="text-xs text-slate-500 text-center py-10">No device records available.</p>
              )}
            </div>

            {/* Top Browsers Bar Chart */}
            <div className="p-5 sm:p-6 rounded-2xl bg-slate-900 border border-slate-800 shadow-md space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-semibold text-slate-200 uppercase tracking-wider flex items-center gap-2">
                  <Compass className="w-4 h-4 text-teal-400" />
                  <span>Top Browsers</span>
                </h3>
                <span className="text-xs text-slate-500 font-mono">Total breakdown</span>
              </div>

              {browserData.length > 0 ? (
                <div className="h-64 w-full">
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={browserData.slice(0, 5)} layout="vertical" margin={{ left: 20, right: 20 }}>
                      <XAxis type="number" stroke="#64748B" fontSize={11} />
                      <YAxis type="category" dataKey="name" stroke="#94A3B8" fontSize={12} width={80} />
                      <Tooltip
                        contentStyle={{
                          backgroundColor: '#0F172A',
                          borderColor: '#334155',
                          borderRadius: '0.75rem',
                          fontSize: '12px',
                          boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.5)',
                        }}
                        labelStyle={{
                          color: '#94A3B8',
                          fontWeight: 600,
                          marginBottom: '2px',
                        }}
                        itemStyle={{
                          color: '#2DD4BF',
                          fontWeight: 500,
                        }}
                        formatter={(val: unknown) => [`${val}`, 'Clicks']}
                      />
                      <Bar
                        dataKey="value"
                          shape={(props: any) => (
                            <Rectangle
                              {...props}
                              fill={BROWSER_COLORS[props.index % BROWSER_COLORS.length]}
                              radius={[0, 6, 6, 0]}
                            />
                          )}
                      />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              ) : (
                <p className="text-xs text-slate-500 text-center py-10">No browser records available.</p>
              )}
            </div>

            {/* Operating Systems */}
            <div className="p-5 sm:p-6 rounded-2xl bg-slate-900 border border-slate-800 shadow-md space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-semibold text-slate-200 uppercase tracking-wider flex items-center gap-2">
                  <Cpu className="w-4 h-4 text-indigo-400" />
                  <span>Operating Systems</span>
                </h3>
                <span className="text-xs text-slate-500 font-mono">Platforms</span>
              </div>

              {osData.length > 0 ? (
                <div className="space-y-3">
                  {osData.map((os) => {
                    const percentage = analytics.totalClicks > 0
                      ? Math.round((os.value / analytics.totalClicks) * 100)
                      : 0;
                    return (
                      <div key={os.name} className="space-y-1">
                        <div className="flex items-center justify-between text-xs">
                          <span className="font-medium text-slate-300">{os.name}</span>
                          <span className="text-slate-400 font-mono">{os.value} clicks ({percentage}%)</span>
                        </div>
                        <div className="h-2 rounded-full bg-slate-950 overflow-hidden">
                          <div
                            className="h-full bg-linear-to-r from-indigo-500 to-teal-400 rounded-full"
                            style={{ width: `${percentage}%` }}
                          />
                        </div>
                      </div>
                    );
                  })}
                </div>
              ) : (
                <p className="text-xs text-slate-500 text-center py-10">No OS data available.</p>
              )}
            </div>

            {/* Geographic Distribution (Countries) */}
            <div className="p-5 sm:p-6 rounded-2xl bg-slate-900 border border-slate-800 shadow-md space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-semibold text-slate-200 uppercase tracking-wider flex items-center gap-2">
                  <Globe className="w-4 h-4 text-amber-400" />
                  <span>Geographic Distribution (GeoIP)</span>
                </h3>
                <span className="text-xs text-slate-500 font-mono">Countries</span>
              </div>

              {countryData.length > 0 ? (
                <div className="space-y-3">
                  {countryData.map((country) => {
                    const percentage = analytics.totalClicks > 0
                      ? Math.round((country.value / analytics.totalClicks) * 100)
                      : 0;
                    return (
                      <div key={country.name} className="space-y-1">
                        <div className="flex items-center justify-between text-xs">
                          <span className="font-medium text-slate-300">{country.name}</span>
                          <span className="text-slate-400 font-mono">{country.value} clicks ({percentage}%)</span>
                        </div>
                        <div className="h-2 rounded-full bg-slate-950 overflow-hidden">
                          <div
                            className="h-full bg-linear-to-r from-amber-500 to-emerald-400 rounded-full"
                            style={{ width: `${percentage}%` }}
                          />
                        </div>
                      </div>
                    );
                  })}
                </div>
              ) : (
                <p className="text-xs text-slate-500 text-center py-10">No country data available.</p>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Empty State when no code searched */}
      {!analytics && !isLoading && !error && (
        <div className="p-12 text-center rounded-2xl bg-slate-900/40 border border-slate-800/80 max-w-lg mx-auto space-y-3">
          <div className="w-12 h-12 rounded-2xl bg-slate-800 text-slate-400 flex items-center justify-center mx-auto">
            <BarChart2 className="w-6 h-6 text-emerald-400/80" />
          </div>
          <h3 className="text-base font-semibold text-white">No Link Selected</h3>
          <p className="text-xs sm:text-sm text-slate-400">
            Enter a 7-character short code above or click "Stats" from your recent links to inspect click analytics.
          </p>
        </div>
      )}
    </div>
  );
};
