import { useCallback } from 'react';

// Using base64 or public URLs for SFX. 
// For this gritty 1990s prototype, we'll use a mix of synthesized audio and placeholders.
// Synthesizing a harsh terminal beep and paper shuffle using Web Audio API

let audioCtx = null;
const getAudioCtx = () => {
    if (!audioCtx) {
        audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    }
    return audioCtx;
};

export function useAudio() {
    
    const playTerminalBeep = useCallback(() => {
        try {
            const ctx = getAudioCtx();
            if (ctx.state === 'suspended') ctx.resume();
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            osc.type = 'square';
            osc.frequency.setValueAtTime(600, ctx.currentTime);
            osc.frequency.exponentialRampToValueAtTime(300, ctx.currentTime + 0.05);
            gain.gain.setValueAtTime(0.05, ctx.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.05);
            osc.connect(gain);
            gain.connect(ctx.destination);
            osc.start();
            osc.stop(ctx.currentTime + 0.05);
        } catch (e) { console.warn("Audio disabled", e); }
    }, []);

    const playErrorBuzz = useCallback(() => {
        try {
            const ctx = getAudioCtx();
            if (ctx.state === 'suspended') ctx.resume();
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            osc.type = 'sawtooth';
            osc.frequency.setValueAtTime(100, ctx.currentTime);
            gain.gain.setValueAtTime(0.1, ctx.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.3);
            osc.connect(gain);
            gain.connect(ctx.destination);
            osc.start();
            osc.stop(ctx.currentTime + 0.3);
        } catch (e) { console.warn("Audio disabled", e); }
    }, []);

    const playPaperShuffle = useCallback(() => {
        try {
            const ctx = getAudioCtx();
            // Synthesizing white noise for paper shuffle
            if (ctx.state === 'suspended') ctx.resume();
            const bufferSize = ctx.sampleRate * 0.2; // 0.2 seconds
            const buffer = ctx.createBuffer(1, bufferSize, ctx.sampleRate);
            const data = buffer.getChannelData(0);
            for (let i = 0; i < bufferSize; i++) {
                data[i] = Math.random() * 2 - 1;
            }
            const noise = ctx.createBufferSource();
            noise.buffer = buffer;
            
            const filter = ctx.createBiquadFilter();
            filter.type = 'bandpass';
            filter.frequency.value = 1000;
            
            const gain = ctx.createGain();
            gain.gain.setValueAtTime(0.05, ctx.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.2);
            
            noise.connect(filter);
            filter.connect(gain);
            gain.connect(ctx.destination);
            noise.start();
        } catch (e) { console.warn("Audio disabled", e); }
    }, []);

    const playKeystroke = useCallback(() => {
        try {
            const ctx = getAudioCtx();
            if (ctx.state === 'suspended') ctx.resume();
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            osc.type = 'triangle';
            // Randomize pitch slightly for typewriter realism
            const baseFreq = 800 + Math.random() * 400;
            osc.frequency.setValueAtTime(baseFreq, ctx.currentTime);
            osc.frequency.exponentialRampToValueAtTime(100, ctx.currentTime + 0.05);
            
            gain.gain.setValueAtTime(0.05, ctx.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.05);
            
            osc.connect(gain);
            gain.connect(ctx.destination);
            osc.start();
            osc.stop(ctx.currentTime + 0.05);
        } catch (e) { console.warn("Audio disabled", e); }
    }, []);

    const playAmbientDrone = useCallback(() => {
        try {
            const ctx = getAudioCtx();
            if (ctx.state === 'suspended') ctx.resume();
            
            if (window._ambientDroneNode) return () => {}; 
            
            // Use filtered white noise for a much softer "server room HVAC" rumble
            const bufferSize = ctx.sampleRate * 2; // 2 seconds
            const buffer = ctx.createBuffer(1, bufferSize, ctx.sampleRate);
            const data = buffer.getChannelData(0);
            for (let i = 0; i < bufferSize; i++) {
                data[i] = Math.random() * 2 - 1;
            }
            
            const noise = ctx.createBufferSource();
            noise.buffer = buffer;
            noise.loop = true;

            const filter = ctx.createBiquadFilter();
            filter.type = 'lowpass';
            filter.frequency.value = 250; // Deep rumble

            const gain = ctx.createGain();
            gain.gain.value = 0.03; // Even quieter, subtle room tone

            noise.connect(filter);
            filter.connect(gain);
            gain.connect(ctx.destination);

            noise.start();
            
            window._ambientDroneNode = { noise, gain };
            
            return () => {
                gain.gain.value = 0;
                try { noise.stop(); } catch(e) {}
                window._ambientDroneNode = null;
            };
        } catch (e) { 
            console.warn("Audio disabled", e); 
            return () => {}; 
        }
    }, []);

    const playTeletype = useCallback(() => {
        try {
            const ctx = getAudioCtx();
            if (ctx.state === 'suspended') ctx.resume();
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            osc.type = 'square';
            // Fast, mechanical printing sound
            osc.frequency.setValueAtTime(300 + Math.random() * 100, ctx.currentTime);
            gain.gain.setValueAtTime(0.015, ctx.currentTime); // Very quiet
            gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.02); // Extremely short
            
            osc.connect(gain);
            gain.connect(ctx.destination);
            osc.start();
            osc.stop(ctx.currentTime + 0.02);
        } catch (e) { console.warn("Audio disabled", e); }
    }, []);

    return {
        playTerminalBeep,
        playErrorBuzz,
        playPaperShuffle,
        playKeystroke,
        playAmbientDrone,
        playTeletype
    };
}
