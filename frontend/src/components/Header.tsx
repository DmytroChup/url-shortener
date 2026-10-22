import React from 'react';
import { Link2, BarChart2, Zap } from 'lucide-react';

interface HeaderProps {
  activeTab: 'shorten' | 'analytics';
  onTabChange: (tab: 'shorten' | 'analytics') => void;
}

export const Header: React.FC<HeaderProps> = ({ activeTab, onTabChange }) => {
  return (
    <header className="border-b border-slate-800/80 bg-slate-900/60 backdrop-blur-md sticky top-0 z-40">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between">
        {/* Brand */}
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-linear-to-tr from-emerald-600 to-teal-400 p-0.5 shadow-lg shadow-emerald-500/20">
            <div className="w-full h-full bg-slate-950 rounded-[10px] flex items-center justify-center">
              <Link2 className="w-5 h-5 text-emerald-400 transform -rotate-45" />
            </div>
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-bold text-lg tracking-tight text-white font-mono">
                Shorten<span className="text-emerald-400">.link</span>
              </span>
              <span className="text-[10px] font-semibold tracking-wider uppercase px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                v1.0
              </span>
            </div>
            <p className="text-xs text-slate-400 hidden sm:block">
              Fast, reliable & secure links
            </p>
          </div>
        </div>

        {/* Tab Controls */}
        <nav className="flex items-center gap-1.5 p-1 bg-slate-950/80 rounded-xl border border-slate-800" role="tablist">
          <button
            role="tab"
            aria-selected={activeTab === 'shorten'}
            onClick={() => onTabChange('shorten')}
            className={`flex items-center gap-2 px-4 py-1.5 rounded-lg text-sm font-medium transition-all duration-150 cursor-pointer ${
              activeTab === 'shorten'
                ? 'bg-slate-800 text-white shadow-sm border border-slate-700/60'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/50'
            }`}
          >
            <Zap className={`w-4 h-4 ${activeTab === 'shorten' ? 'text-emerald-400' : ''}`} />
            <span>Shorten</span>
          </button>

          <button
            role="tab"
            aria-selected={activeTab === 'analytics'}
            onClick={() => onTabChange('analytics')}
            className={`flex items-center gap-2 px-4 py-1.5 rounded-lg text-sm font-medium transition-all duration-150 cursor-pointer ${
              activeTab === 'analytics'
                ? 'bg-slate-800 text-white shadow-sm border border-slate-700/60'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/50'
            }`}
          >
            <BarChart2 className={`w-4 h-4 ${activeTab === 'analytics' ? 'text-emerald-400' : ''}`} />
            <span>Analytics</span>
          </button>
        </nav>

        {/* Operational Status (Hidden on mobile) */}
        <div className="hidden md:flex items-center gap-2 text-xs text-slate-400">
          <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-emerald-500/10 border border-emerald-500/20 text-emerald-400">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
            <span className="font-medium">All systems operational</span>
          </div>
        </div>
      </div>
    </header>
  );
};
