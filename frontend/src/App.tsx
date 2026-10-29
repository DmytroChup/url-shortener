import { useState, useEffect } from 'react';
import { CheckCircle2 } from 'lucide-react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Header } from './components/Header';
import { ShortenCard } from './components/ShortenCard';
import { MyUrlsView } from './components/MyUrlsView';
import { AnalyticsView } from './components/AnalyticsView';
import { AuthModal } from './components/AuthModal';
import { Footer } from './components/Footer';
import type { ShortenedHistoryItem } from './types';

const STORAGE_KEY = 'url_shortener_history_v1';

function AppContent() {
  const { isAuthenticated } = useAuth();
  const [activeTab, setActiveTab] = useState<'shorten' | 'my-urls' | 'analytics'>('shorten');
  const [selectedCode, setSelectedCode] = useState<string>('');
  const [isAuthModalOpen, setIsAuthModalOpen] = useState<boolean>(false);
  const [authModalMode, setAuthModalMode] = useState<'login' | 'register'>('login');
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!isAuthenticated && activeTab === 'my-urls') {
      setActiveTab('shorten');
      setSelectedCode('');
    }
  }, [isAuthenticated, activeTab]);

  const handleLogout = () => {
    setActiveTab('shorten');
    setSelectedCode('');
    setToastMessage('You have successfully signed out.');
    setTimeout(() => {
      setToastMessage(null);
    }, 3500);
  };

  const [history, setHistory] = useState<ShortenedHistoryItem[]>(() => {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      return stored ? JSON.parse(stored) : [];
    } catch {
      return [];
    }
  });

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(history));
    } catch {
      // Storage full or restricted
    }
  }, [history]);

  const handleAddHistory = (item: ShortenedHistoryItem) => {
    setHistory((prev) => [item, ...prev.filter((i) => i.shortCode !== item.shortCode)]);
  };

  const handleClearHistory = () => {
    setHistory([]);
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      // ignore
    }
  };

  const handleViewAnalytics = (shortCode: string) => {
    setSelectedCode(shortCode);
    setActiveTab('analytics');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleOpenAuthModal = (mode: 'login' | 'register' = 'login') => {
    setAuthModalMode(mode);
    setIsAuthModalOpen(true);
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans selection:bg-emerald-500 selection:text-slate-950">
      <Header
        activeTab={activeTab}
        onTabChange={setActiveTab}
        onOpenAuthModal={() => handleOpenAuthModal('login')}
        onLogout={handleLogout}
      />

      <main className="flex-1 max-w-5xl w-full mx-auto px-4 sm:px-6 py-6 sm:py-10">
        {activeTab === 'shorten' && (
          <ShortenCard
            onViewAnalytics={handleViewAnalytics}
            history={history}
            onAddHistory={handleAddHistory}
            onClearHistory={handleClearHistory}
          />
        )}

        {activeTab === 'my-urls' && (
          <MyUrlsView
            onViewAnalytics={handleViewAnalytics}
            onNavigateToShorten={() => setActiveTab('shorten')}
          />
        )}

        {activeTab === 'analytics' && (
          <AnalyticsView
            initialCode={selectedCode}
            onClearInitialCode={() => setSelectedCode('')}
          />
        )}
      </main>

      <Footer />

      <AuthModal
        key={isAuthModalOpen ? `open-${authModalMode}` : 'closed'}
        isOpen={isAuthModalOpen}
        onClose={() => setIsAuthModalOpen(false)}
        initialMode={authModalMode}
      />

      {toastMessage && (
        <div
          role="status"
          aria-live="polite"
          className="fixed bottom-6 right-6 z-50 flex items-center gap-2.5 px-4 py-3 rounded-2xl bg-slate-900/95 border border-slate-700/80 text-white text-xs sm:text-sm shadow-2xl backdrop-blur-md animate-in fade-in slide-in-from-bottom-3 duration-200"
        >
          <div className="w-6 h-6 rounded-lg bg-emerald-500/20 text-emerald-400 flex items-center justify-center shrink-0">
            <CheckCircle2 className="w-4 h-4" aria-hidden="true" />
          </div>
          <span>{toastMessage}</span>
        </div>
      )}
    </div>
  );
}

function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}

export default App;
