import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useGameStore } from '../../store';
import { useAudio } from '../../hooks/useAudio';

export const NotesWidget = () => {
    const [isOpen, setIsOpen] = useState(false);
    const { globalNotes, setGlobalNotes } = useGameStore();
    const { playKeystroke, playPaperShuffle } = useAudio();

    const handleToggle = () => {
        playPaperShuffle();
        setIsOpen(!isOpen);
    };

    const handleKeyDown = (e) => {
        if (!['Shift', 'Control', 'Alt', 'Meta', 'CapsLock', 'Tab'].includes(e.key)) {
            playKeystroke();
        }
    };

    return (
        <div className="absolute bottom-6 right-6 z-50 flex flex-col items-end">
            <AnimatePresence>
                {isOpen && (
                    <motion.div 
                        initial={{ opacity: 0, y: 20, scale: 0.9 }}
                        animate={{ opacity: 1, y: 0, scale: 1 }}
                        exit={{ opacity: 0, y: 20, scale: 0.9 }}
                        className="mb-4 w-72 md:w-80 h-96 bg-[#fdf5e6] shadow-2xl border border-[#d2c2a3] relative flex flex-col"
                        style={{
                            backgroundImage: 'repeating-linear-gradient(transparent, transparent 23px, #7cb9e8 23px, #7cb9e8 24px)',
                            backgroundPosition: '0 30px'
                        }}
                    >
                        {/* Spiral binding effect */}
                        <div className="absolute top-0 left-4 bottom-0 w-4 border-l-2 border-r-2 border-dashed border-[#d2c2a3] opacity-30 pointer-events-none"></div>
                        
                        <div className="bg-[#d2c2a3] p-2 flex justify-between items-center shadow-sm">
                            <span className="font-ui text-xs font-bold text-zinc-800 tracking-widest uppercase">DETECTIVE'S NOTEPAD</span>
                            <button onClick={handleToggle} className="text-zinc-600 hover:text-black">✕</button>
                        </div>
                        <textarea
                            value={globalNotes}
                            onChange={(e) => setGlobalNotes(e.target.value)}
                            onKeyDown={handleKeyDown}
                            className="flex-1 w-full bg-transparent border-none p-4 pl-12 font-ui text-zinc-900 text-sm focus:outline-none resize-none leading-[24px]"
                            placeholder="Jot down clues, contradictions, and suspicions..."
                            spellCheck="false"
                            autoFocus
                        />
                    </motion.div>
                )}
            </AnimatePresence>
            <button 
                onClick={handleToggle}
                className="w-14 h-14 bg-folder-tab border-2 border-folder-manila text-zinc-900 font-terminal font-bold text-xs rounded-full shadow-[0_0_15px_rgba(251,191,36,0.3)] flex items-center justify-center hover:scale-110 hover:bg-terminal-amber transition-transform"
            >
                NOTES
            </button>
        </div>
    );
};
