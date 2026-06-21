import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Layout } from '../components/ui/Layout';
import { useAudio } from '../hooks/useAudio';
import api from '../api';
import { useGameStore } from '../store';

export default function EvidenceBoard() {
    const { caseId } = useParams();
    const navigate = useNavigate();
    const { playPaperShuffle } = useAudio();
    
    const { caseBriefing, suspects, status, verdict, setCaseData, setSuspects } = useGameStore();
    const [loading, setLoading] = useState(!caseBriefing);

    useEffect(() => {
        if (status && status !== 'active') {
            navigate(`/case/${caseId}/verdict`, { state: { verdict } });
            return;
        }
        
        if (!caseBriefing) {
            api.get(`/cases/${caseId}`).then(res => {
                if (res.data.status && res.data.status !== 'active') {
                    navigate(`/case/${caseId}/verdict`, { state: { verdict: res.data.verdict } });
                    return;
                }
                setCaseData(res.data);
                setSuspects(res.data.suspects);
                setLoading(false);
            }).catch(err => {
                console.error(err);
                navigate('/');
            });
        }
    }, [caseId, caseBriefing, status, verdict, setCaseData, setSuspects, navigate]);

    if (loading) {
        return <Layout><div className="flex h-full items-center justify-center font-terminal animate-pulse text-zinc-500">Retrieving files...</div></Layout>;
    }

    const suspect = suspects && suspects.length > 0 ? suspects[0] : null;

    return (
        <Layout hideNotes={true}>
            <motion.div 
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                className="p-8 pb-32 max-w-5xl mx-auto"
            >
                <div className="flex justify-between items-end mb-8 border-b border-zinc-800 pb-4">
                    <div>
                        <h1 className="text-4xl font-terminal text-zinc-100 mb-2">EVIDENCE BOARD</h1>
                        <p className="text-zinc-500 font-terminal">CASE_ID: {caseId.split('-')[0]}</p>
                    </div>
                </div>

                <div className="mb-12 relative overflow-hidden bg-[#e6ddc5] text-zinc-900 p-8 rounded-sm shadow-[2px_2px_10px_rgba(0,0,0,0.5)] border border-[#c4b998]">
                    <div className="absolute top-4 right-4 opacity-10 pointer-events-none w-48 h-48 flex items-center justify-center">
                        <svg viewBox="0 0 100 100" fill="currentColor" className="w-full h-full text-zinc-900">
                            <path d="M50 0 L90 20 L90 60 C90 85 50 100 50 100 C50 100 10 85 10 60 L10 20 Z" stroke="currentColor" strokeWidth="2" fill="none" />
                            <circle cx="50" cy="45" r="15" stroke="currentColor" strokeWidth="2" fill="none" />
                            <path d="M40 45 L60 45 M50 35 L50 55" stroke="currentColor" strokeWidth="2" />
                            <text x="50" y="75" fontSize="8" textAnchor="middle" fontWeight="bold">NYPD</text>
                            <text x="50" y="85" fontSize="6" textAnchor="middle">DETECTIVE BUREAU</text>
                        </svg>
                    </div>
                    <div className="relative z-10">
                        <div className="border-b-2 border-zinc-800 pb-2 mb-6 flex justify-between items-end">
                            <h2 className="text-2xl font-bold font-serif tracking-widest uppercase">Confidential Briefing</h2>
                            <span className="font-mono text-sm font-bold">CASE: {caseId.split('-')[0]}</span>
                        </div>
                        <div className="font-serif leading-relaxed max-w-4xl space-y-6">
                            <div>
                                <strong className="font-bold uppercase text-xs tracking-wider border-b border-zinc-400 pb-1 mb-2 block">Victim Profile</strong>
                                <p className="text-lg">{caseBriefing?.founderName} (Age: {caseBriefing?.founderAge}) — {caseBriefing?.founderTitle} at {caseBriefing?.founderCompany}</p>
                            </div>
                            <div>
                                <strong className="font-bold uppercase text-xs tracking-wider border-b border-zinc-400 pb-1 mb-2 block">Initial Discovery / Police Statement</strong>
                                <p className="text-lg whitespace-pre-wrap">{caseBriefing?.policeStatement}</p>
                            </div>
                            {caseBriefing?.whatWeKnow && (
                                <div>
                                    <strong className="font-bold uppercase text-xs tracking-wider border-b border-zinc-400 pb-1 mb-2 block">What We Know</strong>
                                    <ul className="list-disc pl-5 text-lg space-y-2">
                                        {caseBriefing.whatWeKnow.map((fact, idx) => (
                                            <li key={idx}>{fact}</li>
                                        ))}
                                    </ul>
                                </div>
                            )}
                            <div>
                                <strong className="font-bold uppercase text-xs tracking-wider border-b border-zinc-400 pb-1 mb-2 block">Victim Background</strong>
                                <p className="text-lg whitespace-pre-wrap">{caseBriefing?.pitchDeckSummary}</p>
                            </div>
                        </div>
                    </div>
                </div>

                {suspect && (
                    <>
                        <h2 className="text-2xl font-terminal text-zinc-100 mb-6 border-b border-zinc-800 pb-2">PRIME SUSPECT</h2>
                        
                        <motion.div 
                            initial={{ opacity: 0, y: 20 }}
                            animate={{ opacity: 1, y: 0 }}
                            transition={{ delay: 0.2 }}
                            className="w-full max-w-2xl mx-auto"
                        >
                            <div className="flex flex-col bg-[#050505] border border-zinc-800 shadow-[0_0_15px_rgba(0,0,0,0.8)] hover:border-terminal-green/50 transition-colors p-1 relative group">
                                <div className="absolute top-0 left-0 w-full h-1 bg-zinc-800 group-hover:bg-terminal-green transition-colors"></div>
                                
                                <div className="p-6 flex flex-col md:flex-row gap-6 flex-1 border border-zinc-900 bg-zinc-950">
                                    <div className="w-32 h-32 md:w-48 md:h-48 bg-[#001100] border border-terminal-green flex-shrink-0 flex items-center justify-center overflow-hidden relative shadow-[0_0_10px_rgba(74,222,128,0.3)]">
                                        <img 
                                            src={`https://api.dicebear.com/9.x/pixel-art/svg?seed=${suspect.avatarSeed}`} 
                                            alt={suspect.name} 
                                            className="w-full h-full mix-blend-screen opacity-80"
                                            style={{ filter: 'grayscale(100%) sepia(100%) hue-rotate(80deg) brightness(1.2) contrast(1.5)' }}
                                        />
                                        <div className="absolute inset-0 bg-[linear-gradient(rgba(0,0,0,0)_50%,rgba(0,0,0,0.5)_50%)] bg-[length:100%_4px] pointer-events-none opacity-50"></div>
                                    </div>

                                    <div className="flex-1 flex flex-col justify-between">
                                        <div>
                                            <h3 className="text-2xl font-bold text-zinc-100 uppercase tracking-widest mb-1">{suspect.name}</h3>
                                            <p className="text-sm font-terminal text-terminal-amber mb-4">ID: SYS94-{suspect.id.split('-')[0]}</p>
                                            
                                            <div className="text-sm text-zinc-400 font-terminal space-y-3">
                                                <div className="flex justify-between border-b border-zinc-900 pb-1">
                                                    <span className="text-zinc-600">ROLE</span>
                                                    <span className="text-zinc-300 uppercase">{suspect.title}</span>
                                                </div>
                                                <div className="flex justify-between border-b border-zinc-900 pb-1">
                                                    <span className="text-zinc-600">LINK</span>
                                                    <span className="text-zinc-300 uppercase text-right max-w-[200px] truncate" title={suspect.relationshipToFounder}>
                                                        {suspect.relationshipToFounder}
                                                    </span>
                                                </div>
                                                <div className="flex justify-between pb-1">
                                                    <span className="text-zinc-600">STATUS</span>
                                                    <span className="text-terminal-amber animate-pulse uppercase">PENDING_INT</span>
                                                </div>
                                            </div>
                                        </div>

                                        <div className="mt-6">
                                            <button 
                                                className="w-full py-4 border-2 border-zinc-700 bg-zinc-900 text-zinc-400 font-terminal hover:border-terminal-green hover:text-terminal-green transition-colors text-lg tracking-widest flex items-center justify-center group-hover:bg-black" 
                                                onClick={() => {
                                                    playPaperShuffle();
                                                    navigate(`/case/${caseId}/interrogate/${suspect.id}`);
                                                }}
                                            >
                                                <span className="mr-2">&gt;</span> INTERROGATE <span className="ml-1 opacity-0 group-hover:opacity-100 animate-blink">_</span>
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </motion.div>
                    </>
                )}
            </motion.div>
        </Layout>
    );
}
