import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { Layout, GlassPanel, RetroButton } from '../components/ui/Layout';
import { useAudio } from '../hooks/useAudio';
import api from '../api';
import { useGameStore } from '../store';

export default function InterrogationRoom() {
    const { caseId, suspectId } = useParams();
    const navigate = useNavigate();
    const { token, suspects, caseBriefing, status, verdict } = useGameStore();
    const { playTerminalBeep, playErrorBuzz, playPaperShuffle, playKeystroke, playAmbientDrone, playTeletype } = useAudio();
    
    const suspect = suspects.find(s => s.id === suspectId) || suspects[0];
    
    const [messages, setMessages] = useState([]);
    const [input, setInput] = useState('');
    const [isStreaming, setIsStreaming] = useState(false);
    
    const [caseArgument, setCaseArgument] = useState('');
    const [savingFolder, setSavingFolder] = useState(false);
    const [overlay, setOverlay] = useState(null); // null | 'briefing' | 'background'
    
    // Evidence Room State
    const [rightTab, setRightTab] = useState('file'); // 'file' | 'evidence'
    const [searchQuery, setSearchQuery] = useState('');
    const [searchError, setSearchError] = useState('');
    const [strikes, setStrikes] = useState(0);
    const [locker, setLocker] = useState([]);
    const [isSearching, setIsSearching] = useState(false);
    const [hasConfessed, setHasConfessed] = useState(false);

    // New State
    const [unreadEvidenceCount, setUnreadEvidenceCount] = useState(0);
    const [showEvidenceDrawer, setShowEvidenceDrawer] = useState(false);
    const [endModal, setEndModal] = useState(null); // 'win' | 'loss' | null

    const messagesEndRef = useRef(null);
    const chatContainerRef = useRef(null);
    const inputRef = useRef(null);

    useEffect(() => {
        const stopDrone = playAmbientDrone();
        return () => {
            if (stopDrone) stopDrone();
        };
    }, [playAmbientDrone]);

    useEffect(() => {
        if (status && status !== 'active') {
            navigate(`/case/${caseId}/verdict`, { state: { verdict } });
            return;
        }

        if (!suspect) {
            navigate(`/case/${caseId}`);
            return;
        }

        api.get(`/cases/${caseId}/suspects/${suspectId}/conversation`).then(res => {
            setMessages(res.data.messages || []);
        }).catch(err => console.error("Failed to load conversation", err));

        api.get(`/cases/${caseId}/suspects/${suspectId}/folder`).then(res => {
            setCaseArgument(res.data.contradictions || '');
        }).catch(err => console.error("Failed to load folder", err));

        api.get(`/cases/${caseId}/evidence`).then(res => {
            if (res.data && res.data.length > 0) {
                setLocker(res.data);
                // Set unread count based on how many haven't been presented
                const unread = res.data.filter(e => !e.presented).length;
                setUnreadEvidenceCount(unread);
            }
        }).catch(err => console.error("Failed to load evidence locker", err));

    }, [caseId, suspectId, suspect, navigate, status, verdict]);

    useEffect(() => {
        if (chatContainerRef.current) {
            chatContainerRef.current.scrollTop = chatContainerRef.current.scrollHeight;
        }
    }, [messages]);

    const handleKeyDown = (e) => {
        if (!['Shift', 'Control', 'Alt', 'Meta', 'CapsLock', 'Tab'].includes(e.key)) {
            playKeystroke();
        }
    };

    const playClick = () => {
        try { playTerminalBeep(); } catch(e) {}
    };

    const handleSearch = async (e) => {
        e.preventDefault();
        if (!searchQuery.trim() || isSearching || strikes >= 2) {
            return;
        }
        playClick();
        setIsSearching(true);
        setSearchError('');
        try {
            const res = await api.post(`/cases/${caseId}/evidence/search`, { query: searchQuery });
            const data = res.data;
            
            if (data.strikeCount !== undefined) {
                setStrikes(data.strikeCount);
            }

            if (data.alreadyFound) {
                playErrorBuzz();
                setSearchError('MOST RELEVANT EVIDENCE HAS ALREADY BEEN PROVIDED');
                setTimeout(() => setSearchError(''), 3000);
            } else if (data.matchFound && data.evidence) {
                playTerminalBeep();
                
                const exists = locker.find(e => e.id === data.evidence.id);
                if (!exists) {
                    setLocker(prev => [data.evidence, ...prev]);
                    if (!showEvidenceDrawer) {
                        setUnreadEvidenceCount(c => c + 1);
                    }
                }
                
                setSearchQuery('');
            } else {
                playErrorBuzz();
                setSearchError('NO EVIDENCE FOUND IN DATABASE');
                setTimeout(() => setSearchError(''), 3000);
            }
        } catch (err) {
            console.error("Search API failed:", err);
            playErrorBuzz();
            setSearchError('DATABASE CONNECTION ERROR');
            setTimeout(() => setSearchError(''), 3000);
        } finally {
            setIsSearching(false);
        }
    };

    const handleStrike = (backendStrikes) => {
        playErrorBuzz();
        const newStrikes = backendStrikes !== undefined ? backendStrikes : strikes + 1;
        setStrikes(newStrikes);
        if (newStrikes >= 2) {
            setEndModal('loss');
        }
    };

    const handlePresent = async (evidenceId, evidenceName) => {
        playClick();
        try {
            const res = await api.post(`/cases/${caseId}/evidence/${evidenceId}/present`);
            const data = res.data;
            
            if (data.strikeCount !== undefined) {
                setStrikes(data.strikeCount);
            }

            if (data.success) {
                playPaperShuffle();
                setLocker(prev => prev.map(e => e.id === evidenceId ? { ...e, presented: true } : e));
                
                setInput(prev => {
                    const tag = `[PRESENTS EVIDENCE: ${evidenceName}]`;
                    return prev ? `${prev} ${tag}` : tag;
                });
                setShowEvidenceDrawer(false); // Close drawer to see the chat
                inputRef.current?.focus();
            } else {
                playErrorBuzz();
                setMessages(prev => [...prev, { 
                    role: 'npc', 
                    content: data.npcMessage || "What is this supposed to mean? I'm calling my lawyer if you keep wasting my time." 
                }]);
                setShowEvidenceDrawer(false);
                handleStrike(data.strikeCount);
            }
        } catch (err) {
            console.error(err);
            playErrorBuzz();
            handleStrike();
        }
    };

    const handleSendMessage = async (e) => {
        e.preventDefault();
        if (!input.trim() || isStreaming) return;

        const playerMsg = input;
        setInput('');

        try {
            setIsStreaming(true);
            setMessages(prev => [...prev, { role: 'user', content: playerMsg }]);
            try { playTerminalBeep(); } catch(e) { console.warn('Audio failed', e); }

            const response = await fetch(`/api/cases/${caseId}/suspects/${suspectId}/interrogate`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({ message: playerMsg })
            });

            if (!response.ok) throw new Error("Stream failed");

            const reader = response.body.getReader();
            const decoder = new TextDecoder();
            let currentAssistantMessage = "";
            let buffer = "";
            let streamCompleted = false;

            while (!streamCompleted) {
                const { done, value } = await reader.read();
                if (done) break;
                
                buffer += decoder.decode(value, { stream: true });
                
                while (buffer.includes('\n\n') || buffer.includes('\r\n\r\n')) {
                    const separator = buffer.includes('\r\n\r\n') ? '\r\n\r\n' : '\n\n';
                    const eventIndex = buffer.indexOf(separator);
                    const eventStr = buffer.slice(0, eventIndex);
                    buffer = buffer.slice(eventIndex + separator.length);
                    
                    let eventName = 'message';
                    let dataLines = [];
                    
                    for (const line of eventStr.split(/\r?\n/)) {
                        if (line.startsWith('event:')) {
                            eventName = line.substring(6).trim();
                        } else if (line.startsWith('data:')) {
                            dataLines.push(line.startsWith('data: ') ? line.substring(6) : line.substring(5));
                        }
                    }
                    
                    if (eventName === 'interruption') {
                        const npcMsg = dataLines.join('\n');
                        if (npcMsg.trim()) {
                            playErrorBuzz();
                            setMessages(prev => [...prev, { role: 'npc', content: npcMsg }]);
                        }
                    } else if (eventName === 'token') {
                        const tokenStr = dataLines.join('\n');
                        currentAssistantMessage += tokenStr;
                        setMessages(prev => {
                            const newMsgs = [...prev];
                            if (newMsgs.length === 0 || newMsgs[newMsgs.length - 1].role !== 'assistant') {
                                newMsgs.push({ role: 'assistant', content: currentAssistantMessage });
                            } else {
                                newMsgs[newMsgs.length - 1] = {
                                    ...newMsgs[newMsgs.length - 1],
                                    content: currentAssistantMessage
                                };
                            }
                            return newMsgs;
                        });
                        if (Math.random() > 0.3) playTeletype();
                    } else if (eventName === 'done') {
                        streamCompleted = true;
                        await reader.cancel();
                        break;
                    } else if (eventName === 'error') {
                        throw new Error(dataLines.join('\n') || 'Interrogation failed');
                    }
                }
            }
        } catch (err) {
            console.error(err);
            playErrorBuzz();
        } finally {
            setIsStreaming(false);
            
            if (locker.filter(e => e.presented).length >= 3) {
                setHasConfessed(true);
                setEndModal('win');
            } else {
                setTimeout(() => inputRef.current?.focus(), 0);
            }
        }
    };

    const confirmEndGame = () => {
        playClick();
        if (endModal === 'win') {
            api.post(`/cases/${caseId}/accuse`, { suspectId }).then(res => {
                useGameStore.setState({ status: res.data.status, verdict: res.data.verdict || res.data });
                navigate(`/case/${caseId}/verdict`, { state: { verdict: res.data.verdict || res.data } });
            }).catch(err => {
                console.error(err);
            });
        } else {
            // Loss
            const lostVerdict = {
                isVictory: false,
                victory: false,
                narrative: "The suspect called their lawyer after you harassed them with baseless accusations and irrelevant searches. The Chief has pulled you off the case."
            };
            useGameStore.setState({ status: 'closed', verdict: lostVerdict });
            navigate(`/case/${caseId}/verdict`, { state: { verdict: lostVerdict } });
        }
    };

    const toggleEvidenceDrawer = () => {
        playClick();
        if (!showEvidenceDrawer) {
            setUnreadEvidenceCount(0); // Clear badge
            playPaperShuffle();
        }
        setShowEvidenceDrawer(!showEvidenceDrawer);
    };

    return (
        <Layout hideNotes={true}>
            <div className="h-full flex flex-col md:flex-row gap-6 p-6 overflow-hidden relative">
                
                {/* Left Side: Chat Terminal */}
                <div className="flex-1 flex flex-col relative min-h-0">
                    <div className="flex justify-between items-center mb-2">
                        <RetroButton variant="default" className="text-sm px-4 py-1" onClick={() => { playClick(); navigate(`/case/${caseId}`); }}>
                            [ &lt; BACK TO BOARD ]
                        </RetroButton>
                        <span className="font-terminal text-terminal-green animate-pulse">RECORDING ACTIVE...</span>
                    </div>
                    
                    <GlassPanel className="flex-1 flex flex-col p-4 overflow-hidden border-zinc-800 bg-[#020202]">
                        <div 
                            ref={chatContainerRef}
                            className="flex-1 overflow-y-auto space-y-6 pr-4 pb-4 font-terminal text-lg"
                        >
                            <div className="text-zinc-600 uppercase text-sm mb-8">
                                <p>NYPD INTERROGATION RECORDING SYSTEM V.4.0</p>
                                <p>ESTABLISHED SECURE LINK TO ROOM 4B</p>
                                <p>SUBJECT: {suspect?.name}</p>
                                <p>===========================================</p>
                            </div>

                            {messages.map((msg, idx) => (
                                <motion.div 
                                    key={idx}
                                    initial={{ opacity: 0 }}
                                    animate={{ opacity: 1 }}
                                    className="flex flex-col w-full"
                                >
                                    {msg.role === 'user' ? (
                                        <div className="text-terminal-green">
                                            <span className="opacity-50 mr-4">DETECTIVE &gt;</span>
                                            <span className="uppercase">{msg.content}</span>
                                        </div>
                                    ) : msg.role === 'npc' ? (
                                        <div className="text-blood-red my-4 border-l-4 border-blood-red pl-4 py-2 bg-blood-red/10 animate-pulse">
                                            <span className="font-bold tracking-widest block mb-1">SYSTEM OVERRIDE // LT. HARRIS:</span>
                                            <span className="uppercase tracking-wider">{msg.content}</span>
                                        </div>
                                    ) : (
                                        <div className="text-zinc-300 mt-2 ml-4 border-l border-zinc-700 pl-4 py-1">
                                            <span className="text-terminal-amber opacity-80 mr-4 block text-sm mb-1">{suspect?.name?.toUpperCase()}:</span>
                                            <span className="text-zinc-400">{msg.content}</span>
                                        </div>
                                    )}
                                </motion.div>
                            ))}
                            {isStreaming && messages.length > 0 && messages[messages.length-1].role === 'user' && (
                                <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="mt-2 ml-4 border-l border-zinc-700 pl-4 py-1">
                                    <span className="text-terminal-amber opacity-80 mr-4 block text-sm mb-1">{suspect?.name?.toUpperCase()}:</span>
                                    <span className="animate-blink text-zinc-400">_</span>
                                </motion.div>
                            )}
                            <div ref={messagesEndRef} />
                        </div>
                        
                        {!hasConfessed ? (
                            <form onSubmit={handleSendMessage} className="mt-4 border-t border-zinc-800 pt-4 flex items-center bg-[#050505]">
                                <span className="text-terminal-green font-terminal text-xl mr-2 whitespace-nowrap">C:\NYPD\INT&gt;</span>
                                <input 
                                    ref={inputRef}
                                    type="text" 
                                    value={input}
                                    onChange={(e) => setInput(e.target.value)}
                                    onKeyDown={handleKeyDown}
                                    placeholder=""
                                    className="flex-1 bg-transparent border-none text-terminal-green font-terminal text-xl focus:outline-none disabled:opacity-50 uppercase"
                                    autoFocus
                                    disabled={isStreaming}
                                />
                                {!isStreaming && <span className="text-terminal-green animate-blink font-terminal text-xl">_</span>}
                                <button type="submit" className="hidden">SEND</button>
                            </form>
                        ) : (
                            <div className="mt-4 border-t border-zinc-800 pt-4 flex items-center justify-center bg-[#050505]">
                                <span className="text-terminal-amber font-terminal animate-pulse uppercase">Suspect has confessed. The truth is out.</span>
                            </div>
                        )}
                    </GlassPanel>
                </div>

                {/* Right Side: Tabbed Interface */}
                <div className="w-full md:w-[450px] flex flex-col gap-4">
                    <div className="flex flex-col gap-2 bg-zinc-900 p-3 border border-zinc-800 shadow-md">
                        <div className="flex items-center justify-between">
                            <span className="font-terminal text-sm text-zinc-400">LAWYER UP (ERRORS):</span>
                            <div className="flex gap-2">
                                {[0, 1].map(i => (
                                    <div key={i} className={`w-6 h-6 border flex items-center justify-center font-bold text-sm ${i < strikes ? 'bg-blood-red border-blood-red text-black animate-pulse shadow-[0_0_10px_rgba(220,38,38,0.5)]' : 'bg-zinc-950 border-zinc-700 text-zinc-800'}`}>
                                        X
                                    </div>
                                ))}
                            </div>
                        </div>
                        <div className="flex items-center justify-between">
                            <span className="font-terminal text-sm text-zinc-400">EVIDENCE RELEVANCE:</span>
                            <div className="flex gap-2">
                                {[0, 1, 2].map(i => {
                                    const presentedCount = locker.filter(e => e.presented).length;
                                    return (
                                        <div key={i} className={`w-6 h-6 border flex items-center justify-center font-bold text-sm ${i < presentedCount ? 'bg-terminal-green border-terminal-green text-black shadow-[0_0_10px_rgba(34,197,94,0.5)]' : 'bg-zinc-950 border-zinc-700 text-zinc-800'}`}>
                                            !
                                        </div>
                                    );
                                })}
                            </div>
                        </div>
                    </div>

                    <GlassPanel className="flex-1 bg-[#020202] border-zinc-800 text-zinc-100 flex flex-col">
                        
                        <div className="flex mb-4 border-b border-zinc-800 shrink-0">
                            <button 
                                className={`flex-1 font-terminal tracking-widest text-lg py-3 transition-colors ${rightTab === 'file' ? 'text-folder-tab border-b-2 border-folder-tab bg-folder-manila/5' : 'text-zinc-600 hover:text-zinc-400'}`}
                                onClick={() => { playClick(); setRightTab('file'); }}
                            >
                                SUSPECT_FILE
                            </button>
                            <button 
                                className={`flex-1 font-terminal tracking-widest text-lg py-3 transition-colors ${rightTab === 'evidence' ? 'text-terminal-amber border-b-2 border-terminal-amber bg-terminal-amber/5' : 'text-zinc-600 hover:text-zinc-400'}`}
                                onClick={() => { playClick(); setRightTab('evidence'); }}
                            >
                                SEARCH_DB
                            </button>
                        </div>

                        {rightTab === 'file' ? (
                            <div className="flex-1 flex flex-col p-2">
                                <div className="mb-4 text-center">
                                    <h2 className="text-xl font-terminal text-folder-tab font-bold tracking-widest uppercase">{suspect?.name}</h2>
                                    <p className="text-sm font-terminal opacity-70 text-zinc-400 uppercase">{suspect?.title}</p>
                                </div>
                                
                                <div className="flex gap-2 mb-4">
                                    <RetroButton 
                                        variant="default" 
                                        className="flex-1 text-xs py-2 border-terminal-amber text-terminal-amber hover:bg-terminal-amber hover:text-black"
                                        onClick={() => { playClick(); setOverlay('briefing'); }}
                                    >
                                        CASE BRIEFING
                                    </RetroButton>
                                    <RetroButton 
                                        variant="default" 
                                        className="flex-1 text-xs py-2 border-terminal-green text-terminal-green hover:bg-terminal-green hover:text-black"
                                        onClick={() => { playClick(); setOverlay('background'); }}
                                    >
                                        BACKGROUND CHECK
                                    </RetroButton>
                                </div>
                            </div>
                        ) : (
                            <div className="flex-1 flex flex-col p-2">
                                <div className="mb-6">
                                    <h3 className="text-terminal-amber font-terminal text-lg mb-2">&gt; SEARCH DATABASE</h3>
                                    <form onSubmit={handleSearch} className="flex gap-2">
                                        <input 
                                            type="text" 
                                            value={searchQuery}
                                            onChange={(e) => setSearchQuery(e.target.value)}
                                            onKeyDown={handleKeyDown}
                                            placeholder="e.g. bank statement"
                                            className="flex-1 bg-zinc-950 border border-zinc-800 text-terminal-green p-2 font-terminal focus:border-terminal-green focus:outline-none uppercase"
                                            disabled={strikes >= 2 || hasConfessed}
                                        />
                                        <RetroButton type="submit" disabled={isSearching || strikes >= 2 || hasConfessed}>
                                            {isSearching ? '...' : 'SEARCH'}
                                        </RetroButton>
                                    </form>
                                    
                                    {searchError && (
                                        <div className="mt-2 text-blood-red font-terminal text-sm animate-pulse">
                                            {searchError}
                                        </div>
                                    )}
                                </div>

                                <div className="flex flex-col border-t border-zinc-800 pt-6 items-center mb-4">
                                    <div className="relative w-full">
                                        <RetroButton 
                                            variant="default" 
                                            className="w-full py-3 text-lg bg-zinc-900 border-zinc-700 hover:border-terminal-green hover:text-terminal-green transition-all"
                                            onClick={toggleEvidenceDrawer}
                                        >
                                            [ OPEN EVIDENCE LOCKER ]
                                        </RetroButton>
                                        {unreadEvidenceCount > 0 && (
                                            <div className="absolute -top-2 -right-2 w-6 h-6 rounded-full bg-terminal-green text-black flex items-center justify-center font-bold text-sm border-2 border-zinc-900 animate-bounce shadow-[0_0_15px_rgba(34,197,94,0.6)]">
                                                {unreadEvidenceCount}
                                            </div>
                                        )}
                                    </div>
                                    <p className="mt-3 font-terminal text-xs text-zinc-500 italic">
                                        All discovered evidence is stored securely here.
                                    </p>
                                </div>
                            </div>
                        )}
                    </GlassPanel>
                </div>
            </div>

            {/* Evidence Locker Slide-out Drawer */}
            <AnimatePresence>
                {showEvidenceDrawer && (
                    <motion.div 
                        initial={{ x: '100%' }}
                        animate={{ x: 0 }}
                        exit={{ x: '100%' }}
                        transition={{ type: 'spring', damping: 25, stiffness: 200 }}
                        className="absolute right-0 top-0 bottom-0 w-full md:w-[450px] bg-zinc-950 border-l border-zinc-800 z-50 shadow-2xl flex flex-col"
                    >
                        <div className="p-6 border-b border-zinc-800 flex justify-between items-center bg-zinc-900">
                            <h2 className="text-2xl font-terminal text-terminal-green font-bold tracking-widest">EVIDENCE LOCKER</h2>
                            <RetroButton variant="default" className="text-sm px-3 py-1" onClick={toggleEvidenceDrawer}>
                                [ CLOSE ]
                            </RetroButton>
                        </div>
                        <div className="flex-1 overflow-y-auto p-6 space-y-4 bg-[#020202]">
                            {locker.length === 0 ? (
                                <p className="text-zinc-600 font-terminal text-center mt-10">Locker is empty.</p>
                            ) : locker.map(item => (
                                <div key={item.id} className="border border-zinc-700 bg-zinc-900/50 p-4 flex flex-col gap-3">
                                    <span className="font-bold text-terminal-green uppercase tracking-wider text-lg">{item.name}</span>
                                    <span className="text-zinc-300 text-sm font-terminal leading-relaxed">{item.description}</span>
                                    {!hasConfessed && (
                                        <RetroButton 
                                            variant="default" 
                                            className={`w-full py-2 mt-2 transition-colors ${item.presented ? 'border-zinc-700 text-zinc-600 hover:bg-transparent hover:text-zinc-600 cursor-default' : 'border-terminal-green text-terminal-green hover:bg-terminal-green hover:text-black'}`}
                                            onClick={() => !item.presented && handlePresent(item.id, item.name)}
                                        >
                                            {item.presented ? '[ PRESENTED ]' : '[ PRESENT TO SUSPECT ]'}
                                        </RetroButton>
                                    )}
                                </div>
                            ))}
                        </div>
                    </motion.div>
                )}
            </AnimatePresence>

            {/* Briefing/Background Overlay */}
            <AnimatePresence>
                {overlay && (
                    <motion.div 
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        className="absolute inset-0 z-40 flex items-center justify-center"
                        onClick={() => { playClick(); setOverlay(null); }}
                    >
                        <div className="absolute inset-0 bg-zinc-950/95" />
                        <div className="absolute inset-0 opacity-10" style={{
                            backgroundImage: 'repeating-linear-gradient(0deg, transparent, transparent 2px, rgba(0,0,0,0.3) 2px, rgba(0,0,0,0.3) 4px)',
                        }} />
                        
                        <motion.div 
                            initial={{ scale: 0.9, opacity: 0 }}
                            animate={{ scale: 1, opacity: 1 }}
                            exit={{ scale: 0.9, opacity: 0 }}
                            className="relative z-10 max-w-3xl w-full mx-6 max-h-[80vh] overflow-y-auto"
                            onClick={(e) => e.stopPropagation()}
                        >
                            <div className={`border-2 p-8 rounded-sm shadow-2xl ${
                                overlay === 'briefing' 
                                    ? 'border-terminal-amber bg-zinc-900/95 shadow-[0_0_60px_rgba(251,191,36,0.15)]' 
                                    : 'border-terminal-green bg-zinc-900/95 shadow-[0_0_60px_rgba(74,222,128,0.15)]'
                            }`}>
                                <div className={`flex justify-between items-center border-b pb-4 mb-6 ${
                                    overlay === 'briefing' ? 'border-terminal-amber/30' : 'border-terminal-green/30'
                                }`}>
                                    <div className="flex items-center gap-3">
                                        <span className={`w-3 h-3 rounded-full animate-pulse ${
                                            overlay === 'briefing' ? 'bg-terminal-amber' : 'bg-terminal-green'
                                        }`} />
                                        <h2 className={`text-3xl font-terminal tracking-widest ${
                                            overlay === 'briefing' ? 'text-terminal-amber' : 'text-terminal-green'
                                        }`}>
                                            {overlay === 'briefing' ? 'CASE BRIEFING' : 'BACKGROUND CHECK'}
                                        </h2>
                                    </div>
                                    <RetroButton variant="default" className="text-xs px-3 py-1" onClick={() => { playClick(); setOverlay(null); }}>
                                        [ CLOSE ]
                                    </RetroButton>
                                </div>

                                {overlay === 'briefing' ? (
                                    <div className="font-terminal text-terminal-amber/90 space-y-6 text-lg leading-relaxed">
                                        <div>
                                            <span className="text-xs text-zinc-500 block mb-1">&gt; VICTIM_INFO</span>
                                            <p className="text-zinc-300">{caseBriefing?.founderName} ({caseBriefing?.founderAge}), {caseBriefing?.founderTitle} at {caseBriefing?.founderCompany}</p>
                                        </div>
                                        <div>
                                            <span className="text-xs text-zinc-500 block mb-1">&gt; POLICE_STATEMENT</span>
                                            <p className="text-zinc-300 whitespace-pre-wrap">{caseBriefing?.policeStatement}</p>
                                        </div>
                                        {caseBriefing?.whatWeKnow && (
                                            <div>
                                                <span className="text-xs text-zinc-500 block mb-1">&gt; WHAT_WE_KNOW</span>
                                                <ul className="list-disc pl-5 text-zinc-300 space-y-1">
                                                    {caseBriefing.whatWeKnow.map((fact, idx) => (
                                                        <li key={idx}>{fact}</li>
                                                    ))}
                                                </ul>
                                            </div>
                                        )}
                                        <div>
                                            <span className="text-xs text-zinc-500 block mb-1">&gt; BACKGROUND</span>
                                            <p className="text-zinc-300 whitespace-pre-wrap">{caseBriefing?.pitchDeckSummary}</p>
                                        </div>
                                    </div>
                                ) : (
                                    <div className="font-terminal text-terminal-green/90 space-y-6 text-lg leading-relaxed">
                                        <div>
                                            <span className="text-xs text-zinc-500 block mb-1">&gt; SUBJECT</span>
                                            <p className="text-zinc-300">{suspect?.name} — {suspect?.title} at {suspect?.company}</p>
                                        </div>
                                        <div>
                                            <span className="text-xs text-zinc-500 block mb-1">&gt; RELATIONSHIP_TO_VICTIM</span>
                                            <p className="text-zinc-300">{suspect?.relationshipToFounder}</p>
                                        </div>
                                        <div>
                                            <span className="text-xs text-zinc-500 block mb-1">&gt; BACKGROUND_CHECK_RESULTS</span>
                                            <p className="text-zinc-300 whitespace-pre-wrap">{suspect?.backgroundCheck || 'NO RECORDS FOUND IN SYSTEM.'}</p>
                                        </div>
                                    </div>
                                )}
                            </div>
                        </motion.div>
                    </motion.div>
                )}
            </AnimatePresence>

            {/* End Game Modals */}
            <AnimatePresence>
                {endModal && (
                    <motion.div 
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        className="absolute inset-0 z-50 bg-zinc-950/90 flex items-center justify-center p-6 backdrop-blur-sm"
                    >
                        <GlassPanel className={`max-w-md w-full flex flex-col items-center p-8 text-center border-4 ${endModal === 'win' ? 'border-terminal-green bg-terminal-green/10 shadow-[0_0_50px_rgba(34,197,94,0.2)]' : 'border-blood-red bg-blood-red/10 shadow-[0_0_50px_rgba(220,38,38,0.2)]'}`}>
                            <div className={`w-32 h-32 rounded-full overflow-hidden border-4 mb-6 shadow-lg bg-zinc-800 ${endModal === 'win' ? 'border-terminal-green' : 'border-blood-red'}`}>
                                <img src={`https://api.dicebear.com/9.x/pixel-art/svg?seed=lieutenant-strict&skinColor=fcd7b8`} alt="Lieutenant" className="w-full h-full object-cover" />
                            </div>
                            <h3 className={`text-2xl font-terminal font-bold mb-2 ${endModal === 'win' ? 'text-terminal-green' : 'text-blood-red'}`}>LIEUTENANT HARRIS</h3>
                            <p className="text-zinc-300 font-ui mb-8 leading-relaxed text-lg">
                                {endModal === 'win' 
                                    ? `"Good work detective. The suspect has fully cracked. The DA is going to love this case file. Let's lock them up."` 
                                    : `"The suspect called their lawyer! You harassed them with baseless accusations and irrelevant searches. Hand over your badge, detective. You're off the case."`}
                            </p>
                            <div className="flex flex-col w-full gap-4">
                                <RetroButton 
                                    variant={endModal === 'win' ? 'default' : 'danger'} 
                                    onClick={confirmEndGame} 
                                    className={`w-full text-lg py-4 font-bold tracking-widest ${endModal === 'win' ? 'bg-terminal-green text-black hover:bg-terminal-green/80' : ''}`}
                                >
                                    {endModal === 'win' ? '[ GIVE REPORT TO LIEUTENANT ]' : '[ HAND IN YOUR BADGE DETECTIVE ]'}
                                </RetroButton>
                            </div>
                        </GlassPanel>
                    </motion.div>
                )}
            </AnimatePresence>
        </Layout>
    );
}
