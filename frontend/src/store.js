import { create } from 'zustand';

export const useGameStore = create((set) => ({
    // Auth State
    token: localStorage.getItem('token') || null,
    setToken: (token) => {
        localStorage.setItem('token', token);
        set({ token });
    },
    
    // Case State
    caseId: null,
    caseBriefing: null,
    truthDocument: null,
    suspects: [],
    status: 'active',
    verdict: null,
    
    setCaseData: (data) => set({
        caseId: data.id,
        caseBriefing: data.caseBriefing,
        truthDocument: data.truthDocument,
        status: data.status || 'active',
        verdict: data.verdict || null,
    }),
    
    setSuspects: (suspects) => set({ suspects }),
    
    // Notes State
    globalNotes: '',
    setGlobalNotes: (globalNotes) => set({ globalNotes }),
    
    clearGame: () => set({
        caseId: null,
        caseBriefing: null,
        truthDocument: null,
        suspects: [],
        status: 'active',
        verdict: null,
        globalNotes: ''
    })
}));
