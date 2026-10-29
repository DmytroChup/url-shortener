import React from 'react';
import { Link2, BarChart2, Zap, Bookmark, LogIn, LogOut, User as UserIcon } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

interface HeaderProps {
  activeTab: 'shorten' | 'my-urls' | 'analytics';
  onTabChange: (tab: 'shorten' | 'my-urls' | 'analytics') => void;
  onOpenAuthModal: () => void;
  onLogout?: () => void;
}

export const Header: React.FC<HeaderProps> = ({
  activeTab,
  onTabChange,
  onOpenAuthModal,
  onLogout,
}) => {
  const { user, isAuthenticated, logout } = useAuth();

  const handleLogout = async () => {
    await logout();
    onTabChange('shorten');
    onLogout?.();
  };

  const handleTabClick = (tab: 'shorten' | 'my-urls' | 'analytics') => {
    if (tab === 'my-urls' && !isAuthenticated) {
      onOpenAuthModal();
      return;
    }
    onTabChange(tab);
  };

  return (
    <header className="border-b border-slate-800/80 bg-slate-900/60 backdrop-blur-md sticky top-0 z-40">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between gap-2">
        {/* Brand */}
        <div className="flex items-center gap-3 shrink-0">
          <div className="w-10 h-10 rounded-xl bg-linear-to-tr from-emerald-600 to-teal-400 p-0.5 shadow-lg shadow-emerald-500/20">
            <div className="w-full h-full bg-slate-950 rounded-[10px] flex items-center justify-center">
              <Link2 className="w-5 h-5 text-emerald-400 transform -rotate-45" aria-hidden="true" />
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

        {/* Tab Navigation */}
        <nav
          className="flex items-center gap-1 p-1 bg-slate-950/80 rounded-xl border border-slate-800 text-xs sm:text-sm"
          role="tablist"
        >
          <button
            role="tab"
            aria-selected={activeTab === 'shorten'}
            onClick={() => handleTabClick('shorten')}
            className={`flex items-center gap-1.5 px-3 sm:px-4 py-1.5 rounded-lg font-medium transition-all duration-150 cursor-pointer ${
              activeTab === 'shorten'
                ? 'bg-slate-800 text-white shadow-sm border border-slate-700/60'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/50'
            }`}
          >
            <Zap className={`w-3.5 h-3.5 ${activeTab === 'shorten' ? 'text-emerald-400' : ''}`} aria-hidden="true" />
            <span>Shorten</span>
          </button>

          <button
            role="tab"
            aria-selected={activeTab === 'my-urls'}
            onClick={() => handleTabClick('my-urls')}
            className={`flex items-center gap-1.5 px-3 sm:px-4 py-1.5 rounded-lg font-medium transition-all duration-150 cursor-pointer ${
              activeTab === 'my-urls'
                ? 'bg-slate-800 text-white shadow-sm border border-slate-700/60'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/50'
            }`}
          >
            <Bookmark className={`w-3.5 h-3.5 ${activeTab === 'my-urls' ? 'text-emerald-400' : ''}`} aria-hidden="true" />
            <span>My Links</span>
          </button>

          <button
            role="tab"
            aria-selected={activeTab === 'analytics'}
            onClick={() => handleTabClick('analytics')}
            className={`flex items-center gap-1.5 px-3 sm:px-4 py-1.5 rounded-lg font-medium transition-all duration-150 cursor-pointer ${
              activeTab === 'analytics'
                ? 'bg-slate-800 text-white shadow-sm border border-slate-700/60'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/50'
            }`}
          >
            <BarChart2 className={`w-3.5 h-3.5 ${activeTab === 'analytics' ? 'text-emerald-400' : ''}`} aria-hidden="true" />
            <span>Analytics</span>
          </button>
        </nav>

        {/* Right Section: Auth State / Action */}
        <div className="flex items-center gap-2">
          {isAuthenticated && user ? (
            <div className="flex items-center gap-2">
              <div className="hidden sm:flex items-center gap-2 px-3 py-1.5 rounded-xl bg-slate-800/80 border border-slate-700/60 text-xs">
                <div className="w-6 h-6 rounded-lg bg-emerald-500/20 text-emerald-400 flex items-center justify-center font-bold font-mono">
                  {user.email ? user.email.charAt(0).toUpperCase() : <UserIcon className="w-3.5 h-3.5" />}
                </div>
                <div className="text-left">
                  <p className="text-slate-200 font-medium truncate max-w-[120px]" title={user.email}>
                    {user.email || `User #${user.id}`}
                  </p>
                  {user.role === 'ROLE_ADMIN' && (
                    <span className="text-[10px] text-amber-400 font-semibold block leading-tight">Admin</span>
                  )}
                </div>
              </div>

              <button
                onClick={handleLogout}
                className="p-2 sm:px-3 sm:py-1.5 rounded-xl bg-slate-800 hover:bg-red-500/10 text-slate-300 hover:text-red-400 hover:border-red-500/30 border border-slate-700/60 text-xs font-medium transition-colors flex items-center gap-1.5 cursor-pointer"
                title="Sign Out"
                aria-label="Sign Out"
              >
                <LogOut className="w-3.5 h-3.5" aria-hidden="true" />
                <span className="hidden sm:inline">Sign Out</span>
              </button>
            </div>
          ) : (
            <button
              onClick={onOpenAuthModal}
              className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-semibold text-xs sm:text-sm shadow-md shadow-emerald-500/20 transition-all cursor-pointer"
            >
              <LogIn className="w-3.5 h-3.5" aria-hidden="true" />
              <span>Sign In</span>
            </button>
          )}
        </div>
      </div>
    </header>
  );
};
