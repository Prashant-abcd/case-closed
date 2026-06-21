import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { Layout, GlassPanel, RetroButton } from '../components/ui/Layout';
import { useAudio } from '../hooks/useAudio';
import api from '../api';
import { useGameStore } from '../store';

export default function TitleScreen() {
    const navigate = useNavigate();
    const { playTerminalBeep, playPaperShuffle, playErrorBuzz } = useAudio();
    const setCaseData = useGameStore((state) => state.setCaseData);
    const setSuspects = useGameStore((state) => state.setSuspects);
    
    const [loading, setLoading] = useState(false);
    const [statusText, setStatusText] = useState('');
    const [bootPhase, setBootPhase] = useState(0);

    // Run fake boot sequence on mount
    React.useEffect(() => {
        const sequence = async () => {
            await new Promise(r => setTimeout(r, 500));
            setBootPhase(1); // MEMORY CHECK
            await new Promise(r => setTimeout(r, 800));
            setBootPhase(2); // LOADING OS
            await new Promise(r => setTimeout(r, 800));
            setBootPhase(3); // INIT DONE, SHOW LOGO
        };
        sequence();
    }, []);

    const startNewGame = async () => {
        playTerminalBeep();
        setLoading(true);
        setStatusText('INITIALIZING CASE GENERATION ENGINE...');
        
        try {
            setTimeout(() => { playTerminalBeep(); setStatusText('ESTABLISHING SECURE CONNECTION TO CRIMINAL DATABASE...'); }, 1500);
            setTimeout(() => { playTerminalBeep(); setStatusText('GENERATING SUSPECT PROFILES AND TRUTH DOCUMENT...'); }, 3500);
            
            const res = await api.post('/cases');
            const data = res.data;
            
            setCaseData(data);
            setSuspects(data.suspects);
            
            playPaperShuffle();
            navigate(`/case/${data.id}`);
            
        } catch (err) {
            console.error('Failed to generate case:', err);
            playErrorBuzz();
            setStatusText('FATAL ERROR: CONNECTION LOST.');
            setLoading(false);
        }
    };

    if (bootPhase < 3) {
        return (
            <Layout showHeader={false} hideNotes={true}>
                <div className="h-full p-8 font-terminal text-terminal-green text-xl">
                    <p>IBM ROM BIOS Version 2.10</p>
                    <p>Copyright (C) 1981-1994 IBM Corp.</p>
                    <br />
                    {bootPhase >= 1 && <p>640K RAM SYSTEM... OK</p>}
                    {bootPhase >= 2 && <p>LOADING NYPD_SYS.EXE... OK</p>}
                    <span className="animate-blink">_</span>
                </div>
            </Layout>
        );
    }

    return (
        <Layout showHeader={false} hideNotes={true}>
            <div className="h-full flex flex-col items-center justify-center">
                <motion.div 
                    initial={{ opacity: 0, scale: 0.95, filter: 'blur(10px)' }}
                    animate={{ opacity: 1, scale: 1, filter: 'blur(0px)' }}
                    transition={{ duration: 0.5, ease: "easeOut" }}
                    className="text-center w-full max-w-4xl"
                >
                    {/* Giant Glitch Title */}
                    <h1 
                        className="text-7xl md:text-9xl font-terminal font-bold text-zinc-100 tracking-tighter mb-2 text-glitch" 
                        data-text="CASE CLOSED"
                        style={{ textShadow: '0 0 20px rgba(255,255,255,0.5)' }}
                    >
                        CASE CLOSED
                    </h1>
                    <p className="text-xl md:text-2xl font-terminal text-terminal-amber tracking-[0.3em] mb-16 animate-flicker">
                        METROPOLITAN HOMICIDE DIVISION
                    </p>
                    
                    <AnimatePresence mode="wait">
                        {!loading ? (
                            <motion.div 
                                key="button"
                                initial={{ opacity: 0 }}
                                animate={{ opacity: 1 }}
                                exit={{ opacity: 0 }}
                            >
                                <button 
                                    onClick={startNewGame} 
                                    className="group relative font-terminal text-2xl md:text-4xl text-terminal-green hover:text-white transition-colors duration-100"
                                >
                                    <span className="opacity-50 group-hover:opacity-100 mr-2">&gt;</span>
                                    <span className="group-hover:bg-terminal-green group-hover:text-black px-2">ACCEPT_CASE</span>
                                    <span className="animate-blink ml-1">_</span>
                                </button>
                            </motion.div>
                        ) : (
                            <motion.div 
                                key="loading"
                                initial={{ opacity: 0 }}
                                animate={{ opacity: 1 }}
                                className="mt-8 text-left w-full max-w-2xl mx-auto"
                            >
                                <div className="border border-terminal-green/50 bg-black/50 p-6 font-terminal">
                                    <p className="text-terminal-green text-xl">
                                        <span className="mr-4">C:\NYPD\SYS&gt;</span>
                                        <span className="animate-pulse">{statusText}</span>
                                    </p>
                                    <div className="w-full bg-zinc-900 border border-zinc-800 h-4 mt-6 p-1">
                                        <motion.div 
                                            className="h-full bg-terminal-green"
                                            initial={{ width: "0%" }}
                                            animate={{ width: "100%" }}
                                            transition={{ duration: 6, ease: "linear" }}
                                        />
                                    </div>
                                </div>
                            </motion.div>
                        )}
                    </AnimatePresence>
                </motion.div>
            </div>
        </Layout>
    );
}
