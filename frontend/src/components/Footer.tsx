import React from 'react';
import { Code2 } from 'lucide-react';

const CURRENT_YEAR = new Date().getFullYear();

export const Footer: React.FC = () => {
  return (
    <footer className="mt-16 border-t border-slate-800/80 bg-slate-950 py-6 text-slate-400 text-xs">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 flex flex-col sm:flex-row items-center justify-between gap-4 text-slate-500">
        <p>© {CURRENT_YEAR} Shorten.link. Fast, secure, and reliable link management.</p>

        <a
          href="https://github.com/DmytroChup/url-shortener"
          target="_blank"
          rel="noopener noreferrer"
          className="inline-flex items-center gap-1.5 hover:text-slate-300 transition-colors"
        >
          <Code2 className="w-4 h-4" />
          <span>GitHub</span>
        </a>
      </div>
    </footer>
  );
};
