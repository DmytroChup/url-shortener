import { useState, useEffect } from 'react';
import { Header } from './components/Header';
import { ShortenCard } from './components/ShortenCard';
import { AnalyticsView } from './components/AnalyticsView';
import { Footer } from './components/Footer';
import type { ShortenedHistoryItem } from './types';

const STORAGE_KEY = 'url_shortener_history_v1';

function App() {
  const [activeTab, setActiveTab] = useState<'shorten' | 'analytics'>('shorten');
  const [selectedCode, setSelectedCode] = useState<string>('');
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

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans selection:bg-emerald-500 selection:text-slate-950">
      <Header activeTab={activeTab} onTabChange={setActiveTab} />

      <main className="flex-1 max-w-5xl w-full mx-auto px-4 sm:px-6 py-6 sm:py-10">
        {activeTab === 'shorten' ? (
          <ShortenCard
            onViewAnalytics={handleViewAnalytics}
            history={history}
            onAddHistory={handleAddHistory}
            onClearHistory={handleClearHistory}
          />
        ) : (
          <AnalyticsView
            initialCode={selectedCode}
            onClearInitialCode={() => setSelectedCode('')}
          />
        )}
      </main>

      <Footer />
    </div>
  );
}

export default App;
