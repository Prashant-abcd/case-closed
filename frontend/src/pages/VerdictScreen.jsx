import React, { useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { Layout, GlassPanel, RetroButton } from '../components/ui/Layout';
import { useGameStore } from '../store';
import { useAudio } from '../hooks/useAudio';

export default function VerdictScreen() {
    const location = useLocation();
    const navigate = useNavigate();
    const { caseId } = useParams();
    const { playErrorBuzz, playPaperShuffle } = useAudio();
    const { clearGame } = useGameStore();
    
    const verdict = location.state?.verdict;
    const truthDocument = useGameStore(s => s.truthDocument) || verdict?.truthDocument;
    const [showTruth, setShowTruth] = useState(false);

    if (!verdict) {
        return (
            <Layout>
                <div className="h-full flex items-center justify-center font-terminal text-red-500 animate-pulse text-2xl">
                    CORRUPT VERDICT DATA. RETURN TO DESK.
                    <RetroButton onClick={() => navigate(`/case/${caseId}`)} className="ml-4">BACK</RetroButton>
                </div>
            </Layout>
        );
    }

    const isVictory = verdict.isVictory || verdict.victory;

    const handleRevealTruth = () => {
        playPaperShuffle();
        setShowTruth(true);
    };

    const handleNewGame = () => {
        clearGame();
        navigate('/');
    };

    return (
        <Layout>
            <div className="h-full w-full max-w-3xl mx-auto p-6 overflow-y-auto pb-32">
                
                <motion.div 
                    initial={{ opacity: 0, scale: 0.9 }}
                    animate={{ opacity: 1, scale: 1 }}
                    transition={{ duration: 0.5 }}
                >
                    <GlassPanel className={`border-4 ${isVictory ? 'border-terminal-green bg-terminal-green/5' : 'border-blood-red bg-blood-red/5'}`}>
                        <div className="flex flex-col items-center text-center mb-8">
                            <div className={`w-32 h-32 rounded-sm overflow-hidden border-4 mb-6 shadow-2xl bg-zinc-900 ${
                                isVictory 
                                    ? 'border-terminal-green shadow-[0_0_50px_rgba(74,222,128,0.3)]' 
                                    : 'border-blood-red shadow-[0_0_50px_rgba(220,38,38,0.3)]'
                            }`}>
                                <img 
                                    src={`https://api.dicebear.com/9.x/pixel-art/svg?seed=${isVictory ? 'chief-happy-2' : 'chief-angry-5'}&skinColor=fcd7b8`} 
                                    alt="Chief of Police" 
                                    className="w-full h-full object-cover opacity-90" 
                                    style={{ 
                                        filter: isVictory 
                                            ? 'sepia(1) hue-rotate(70deg) saturate(300%) contrast(1.2)' 
                                            : 'sepia(1) hue-rotate(-50deg) saturate(500%) contrast(1.2)' 
                                    }}
                                />
                            </div>
                            <h1 className={`text-6xl font-terminal font-bold mb-4 tracking-widest ${isVictory ? 'text-terminal-green' : 'text-blood-red'}`}>
                                {isVictory ? 'CASE CLOSED' : 'CASE BOTCHED'}
                            </h1>
                            <p className="text-xl font-terminal text-zinc-300">
                                OFFICIAL RULING FROM THE CHIEF OF POLICE
                            </p>
                        </div>

                        <div className="bg-zinc-950 p-6 border border-zinc-800 rounded-sm mb-8">
                            <p className="font-terminal text-lg leading-loose text-zinc-300 whitespace-pre-wrap">
                                {verdict.narrative || verdict.chiefResponse || "The case has been reviewed."}
                            </p>
                        </div>

                        {!showTruth ? (
                            <div className="flex justify-center gap-4">
                                <RetroButton variant="primary" onClick={handleRevealTruth}>
                                    [ DECLASSIFY TRUTH ]
                                </RetroButton>
                                <RetroButton variant="default" onClick={handleNewGame}>
                                    [ NEW CASE ]
                                </RetroButton>
                            </div>
                        ) : null}
                    </GlassPanel>
                </motion.div>

                <AnimatePresence>
                    {showTruth && truthDocument && (
                        <motion.div
                            initial={{ opacity: 0, y: 50 }}
                            animate={{ opacity: 1, y: 0 }}
                            className="mt-12"
                        >
                            <GlassPanel className="border-folder-tab bg-folder-manila/5">
                                <h2 className="text-3xl font-terminal text-folder-tab border-b border-folder-tab/30 pb-4 mb-6">
                                    WHAT ACTUALLY HAPPENED
                                </h2>
                                
                                <div className="space-y-6 font-terminal text-lg">
                                    <div className="bg-zinc-950 p-4 border border-zinc-800 rounded-sm">
                                        <span className="text-xs text-zinc-500 block mb-2">THE KILLER</span>
                                        <p className="text-2xl text-blood-red font-bold">{truthDocument.killerName}</p>
                                    </div>
                                    
                                    <div className="bg-zinc-950 p-4 border border-zinc-800 rounded-sm">
                                        <span className="text-xs text-zinc-500 block mb-2">HOW THEY DID IT</span>
                                        <p className="text-zinc-300"><strong className="text-zinc-100">Weapon:</strong> {truthDocument.weapon}</p>
                                        <p className="text-zinc-300 mt-2">{truthDocument.method}</p>
                                    </div>
                                    
                                    <div className="bg-zinc-950 p-4 border border-zinc-800 rounded-sm">
                                        <span className="text-xs text-zinc-500 block mb-2">WHY</span>
                                        <p className="text-zinc-300">{truthDocument.detailedMotive}</p>
                                    </div>
                                </div>
                                
                                <div className="mt-8 text-center">
                                    <RetroButton variant="default" onClick={handleNewGame}>
                                        [ CLOSE FILE & START NEW CASE ]
                                    </RetroButton>
                                </div>
                            </GlassPanel>
                        </motion.div>
                    )}
                </AnimatePresence>
                
            </div>
        </Layout>
    );
}
