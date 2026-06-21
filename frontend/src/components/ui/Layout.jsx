import React from 'react';
import { NotesWidget } from './NotesWidget';

export const Layout = ({ children, showHeader = false, hideNotes = false }) => {
    return (
        <div className="w-full h-full bg-[#050505] p-2 md:p-8 flex items-center justify-center">
            {/* Monitor Bezel */}
            <div className="relative w-full max-w-[1400px] h-full max-h-[900px] bg-[#111] rounded-2xl md:rounded-[2rem] p-4 md:p-8 shadow-[inset_0_0_20px_rgba(0,0,0,1),0_10px_50px_rgba(0,0,0,0.8)] border-4 border-[#222]">
                
                {/* Brand Logo on Bezel */}
                <div className="absolute bottom-2 left-1/2 transform -translate-x-1/2 text-[#333] font-serif text-xs font-bold tracking-widest pointer-events-none">
                    IBM SYSTEM/94
                </div>

                {/* Power LED */}
                <div className="absolute bottom-3 right-8 flex items-center gap-2 pointer-events-none">
                    <div className="w-2 h-2 rounded-full bg-green-500 shadow-[0_0_10px_rgba(34,197,94,0.8)] animate-pulse"></div>
                    <span className="text-[#333] font-mono text-[8px] uppercase">PWR</span>
                </div>

                {/* The Actual Screen */}
                <div className="relative w-full h-full bg-zinc-950 overflow-hidden rounded-lg md:rounded-xl shadow-[inset_0_0_30px_rgba(0,0,0,1)] border-2 border-black flex flex-col screen-curve">
                    
                    {/* CRT Overlays */}
                    <div className="crt-overlay"></div>
                    <div className="crt-vignette"></div>
                    <div className="crt-glare"></div>
                    
                    {showHeader && (
                        <header className="w-full flex justify-between items-center p-4 border-b border-zinc-800 bg-zinc-950/80 backdrop-blur-md z-10 shadow-md">
                            <div className="font-terminal text-terminal-green text-xl tracking-widest text-glitch" data-text="NYPD HOMICIDE OFFICE">
                                NYPD HOMICIDE OFFICE
                            </div>
                            <div className="font-terminal text-terminal-amber animate-pulse">
                                HEY DETECTIVE, THEY NEED YOU.
                            </div>
                        </header>
                    )}
                    
                    <main className="flex-1 w-full relative z-10 overflow-y-auto">
                        {children}
                    </main>

                    {/* Global Notes Overlay */}
                    {!hideNotes && <NotesWidget />}
                </div>
            </div>
        </div>
    );
};

export const GlassPanel = ({ children, className = '' }) => (
    <div className={`glass-panel rounded-sm p-6 ${className}`}>
        {children}
    </div>
);

export const RetroButton = ({ children, onClick, variant = 'default', className = '', type = 'button', disabled = false, ...props }) => {
    const base = "font-terminal text-lg tracking-wider uppercase px-6 py-2 border transition-all duration-200 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed disabled:active:scale-100";
    
    const variants = {
        default: "border-zinc-700 text-zinc-300 hover:border-zinc-400 hover:text-white bg-zinc-800/50 hover:bg-zinc-700/50",
        primary: "border-terminal-amber text-terminal-amber hover:bg-terminal-amber hover:text-zinc-950 shadow-[0_0_10px_rgba(251,191,36,0.2)] hover:shadow-[0_0_20px_rgba(251,191,36,0.6)]",
        danger: "border-blood-red text-red-400 hover:bg-blood-red hover:text-white"
    };

    return (
        <button 
            type={type}
            onClick={onClick} 
            disabled={disabled}
            className={`${base} ${variants[variant]} ${className}`}
            {...props}
        >
            {children}
        </button>
    );
};
