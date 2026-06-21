import { useEffect, useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useGameStore } from './store';
import api from './api';

// Placeholder Pages
import TitleScreen from './pages/TitleScreen';

import EvidenceBoard from './pages/EvidenceBoard';

import InterrogationRoom from './pages/InterrogationRoom';
import VerdictScreen from './pages/VerdictScreen';

function App() {
  const token = useGameStore((state) => state.token);
  const setToken = useGameStore((state) => state.setToken);
  const [isReady, setIsReady] = useState(false);

  // Create an anonymous guest session on startup.
  useEffect(() => {
    const initAuth = async () => {
      if (token) {
        setIsReady(true);
        return;
      }
      try {
        const res = await api.post('/session');
        setToken(res.data.token);
      } catch (err) {
        console.error('Failed to create guest session:', err);
      } finally {
        setIsReady(true);
      }
    };
    initAuth();
  }, [token, setToken]);

  if (!isReady) {
    return <div className="flex h-screen items-center justify-center font-terminal text-zinc-500 animate-pulse">Initializing Terminal...</div>;
  }

  return (
    <BrowserRouter>
      <div className="h-full w-full text-zinc-300">
        <Routes>
          <Route path="/" element={<TitleScreen />} />
          <Route path="/case/:caseId" element={<EvidenceBoard />} />
          <Route path="/case/:caseId/interrogate/:suspectId" element={<InterrogationRoom />} />
          <Route path="/case/:caseId/verdict" element={<VerdictScreen />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </div>
    </BrowserRouter>
  );
}

export default App;
