'use client';

import { FormEvent, useEffect, useRef, useState } from 'react';
import {
  Activity,
  Brain,
  Check,
  ChevronDown,
  ChevronRight,
  ClipboardPlus,
  Database,
  Eye,
  HeartPulse,
  HelpCircle,
  Info,
  LockKeyhole,
  MessageCircle,
  Mic,
  Moon,
  RotateCcw,
  Send,
  Settings2,
  ShieldCheck,
  Sparkles,
  SunMedium,
  Volume2,
  X,
} from 'lucide-react';

type Factor = {
  title: string;
  resource: string;
  detail: string;
  note: string;
  status: string;
  tone: 'amber' | 'green' | 'blue';
  summary: string;
  action: string;
  icon: typeof Moon;
};

type ChatMessage = { id: number; role: 'assistant' | 'user'; text: string };

type SpeechResultEvent = {
  results: ArrayLike<{ 0: { transcript: string } }>;
};

type SpeechRecognitionInstance = {
  lang: string;
  interimResults: boolean;
  continuous: boolean;
  onresult: (event: SpeechResultEvent) => void;
  onerror: () => void;
  onend: () => void;
  start: () => void;
};

const factors: Factor[] = [
  {
    title: 'Sleep',
    resource: 'Observation',
    detail: '6 h 10 min last night',
    note: 'A small change could help',
    status: 'Worth attention',
    tone: 'amber',
    summary: 'Your sleep has been shorter than your usual pattern on four of the past seven nights.',
    action: 'Try beginning your wind-down routine 20 minutes earlier tonight.',
    icon: Moon,
  },
  {
    title: 'Movement',
    resource: 'Observation',
    detail: '4,280 steps yesterday',
    note: 'You are building momentum',
    status: 'On track',
    tone: 'green',
    summary: 'You moved more on three days this week. Short, regular movement can support sleep and overall wellbeing.',
    action: 'If it feels safe, add a gentle 10-minute walk after lunch.',
    icon: Activity,
  },
  {
    title: 'Mind & connection',
    resource: 'CarePlan',
    detail: '2 activities this week',
    note: 'One more would meet your goal',
    status: 'In progress',
    tone: 'blue',
    summary: 'Your care plan includes three enjoyable social or thinking activities each week.',
    action: 'Call a friend or spend 15 minutes on a familiar puzzle this afternoon.',
    icon: Brain,
  },
];

const quickPrompts = [
  'How can I sleep better?',
  'Suggest gentle movement',
  'Explain my health data',
];

const initialMessages: ChatMessage[] = [
  {
    id: 1,
    role: 'assistant',
    text: 'Hello Margaret. I noticed your sleep has been a little shorter this week. Would you like one simple idea for tonight?',
  },
];

function responseFor(question: string) {
  const lower = question.toLowerCase();
  if (lower.includes('sleep')) {
    return 'A gentle place to start is keeping the hour before bed quiet and predictable. Tonight, try dimming the lights and putting your phone aside 20 minutes earlier. Would you like me to add that as today’s step?';
  }
  if (lower.includes('movement') || lower.includes('walk') || lower.includes('exercise')) {
    return 'Your recent activity is moving in a positive direction. If your care team says walking is safe for you, a comfortable 10-minute walk after lunch could be a useful next step. Stop if you feel unsteady or unwell.';
  }
  if (lower.includes('data') || lower.includes('record') || lower.includes('fhir')) {
    return 'I used four parts of this synthetic health record: basic details from Patient, Parkinson’s history from Condition, sleep and step measures from Observation, and existing goals from CarePlan. This prototype does not store the record locally.';
  }
  return 'I can help explain your sleep, movement, and mind-and-connection patterns in plain language. For personal medical decisions or new symptoms, please speak with your healthcare professional.';
}

export default function Home() {
  const [selectedFactor, setSelectedFactor] = useState<Factor | null>(null);
  const [showAccess, setShowAccess] = useState(false);
  const [showProfile, setShowProfile] = useState(false);
  const [largeText, setLargeText] = useState(false);
  const [highContrast, setHighContrast] = useState(false);
  const [goalDone, setGoalDone] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>(initialMessages);
  const [question, setQuestion] = useState('');
  const [thinking, setThinking] = useState(false);
  const [listening, setListening] = useState(false);
  const [announcement, setAnnouncement] = useState('');
  const messageEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setSelectedFactor(null);
        setShowAccess(false);
        setShowProfile(false);
      }
    };
    window.addEventListener('keydown', closeOnEscape);
    return () => window.removeEventListener('keydown', closeOnEscape);
  }, []);

  useEffect(() => {
    if (messages.length > 1) messageEndRef.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }, [messages, thinking]);

  const sendQuestion = (text: string) => {
    const clean = text.trim();
    if (!clean || thinking) return;
    setMessages((current) => [...current, { id: Date.now(), role: 'user', text: clean }]);
    setQuestion('');
    setThinking(true);
    setAnnouncement('MindStep is preparing a response.');
    window.setTimeout(() => {
      setMessages((current) => [...current, { id: Date.now() + 1, role: 'assistant', text: responseFor(clean) }]);
      setThinking(false);
      setAnnouncement('MindStep has replied.');
    }, 700);
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    sendQuestion(question);
  };

  const startVoiceInput = () => {
    const speechWindow = window as typeof window & {
      webkitSpeechRecognition?: new () => SpeechRecognitionInstance;
      SpeechRecognition?: new () => SpeechRecognitionInstance;
    };
    const Recognition = speechWindow.SpeechRecognition ?? speechWindow.webkitSpeechRecognition;
    if (!Recognition) {
      setAnnouncement('Voice input is not available in this browser. You can type your question instead.');
      return;
    }
    const recognition = new Recognition();
    recognition.lang = 'en-AU';
    recognition.interimResults = false;
    recognition.continuous = false;
    recognition.onresult = (event) => {
      const transcript = event.results[0]?.[0]?.transcript ?? '';
      setQuestion(transcript);
      setAnnouncement(`Voice input captured: ${transcript}`);
    };
    recognition.onerror = () => setAnnouncement('I could not hear that clearly. Please try again or type your question.');
    recognition.onend = () => setListening(false);
    setListening(true);
    setAnnouncement('Listening. Say your question now.');
    recognition.start();
  };

  const readLatestAnswer = () => {
    const latest = [...messages].reverse().find((message) => message.role === 'assistant');
    if (!latest || !('speechSynthesis' in window)) {
      setAnnouncement('Read aloud is not available in this browser.');
      return;
    }
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(latest.text);
    utterance.lang = 'en-AU';
    utterance.rate = 0.88;
    window.speechSynthesis.speak(utterance);
    setAnnouncement('Reading the latest answer aloud.');
  };

  const resetConversation = () => {
    setMessages(initialMessages);
    setQuestion('');
    setThinking(false);
    setAnnouncement('Conversation reset.');
  };

  return (
    <div className={`app-frame${largeText ? ' large-text' : ''}${highContrast ? ' high-contrast' : ''}`}>
      <a className="skip-link" href="#main-content">Skip to main content</a>
      <div className="demo-banner">
        <ShieldCheck aria-hidden="true" size={18} />
        <span>Prototype using a synthetic patient record — no real health data</span>
      </div>

      <header className="topbar">
        <a className="brand" href="#main-content" aria-label="MindStep home">
          <span className="brand-mark"><HeartPulse aria-hidden="true" size={25} /></span>
          <span>MindStep</span>
        </a>
        <nav aria-label="Main navigation">
          <a className="nav-link active" href="#today">Today</a>
          <a className="nav-link" href="#factors">My factors</a>
          <a className="nav-link" href="#ask">Ask MindStep</a>
        </nav>
        <div className="top-actions">
          <button className="icon-label-button" type="button" onClick={() => setShowAccess(true)}>
            <Settings2 aria-hidden="true" size={21} />
            <span>Display</span>
          </button>
          <button className="profile-button" type="button" onClick={() => setShowProfile(true)} aria-haspopup="dialog">
            <span className="avatar" aria-hidden="true">MC</span>
            <span className="profile-name">Margaret</span>
            <ChevronDown aria-hidden="true" size={18} />
          </button>
        </div>
      </header>

      <main className="page-shell" id="main-content">
        <section className="welcome" id="today">
          <div>
            <p className="eyebrow">Wednesday, 2 September</p>
            <h1>Good morning, Margaret.</h1>
            <p className="welcome-copy">Let’s focus on one small, helpful step today.</p>
          </div>
          <button className="ehr-status" type="button" onClick={() => setShowProfile(true)} aria-label="View connected synthetic health record">
            <span className="status-dot" aria-hidden="true" />
            <span><strong>Health record connected</strong><small>Updated 9:14 am · View data</small></span>
            <ChevronRight aria-hidden="true" size={20} />
          </button>
        </section>

        <section aria-labelledby="factor-heading" id="factors">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Your wellbeing snapshot</p>
              <h2 id="factor-heading">Three things worth noticing</h2>
            </div>
            <p className="section-note"><Info aria-hidden="true" size={18} /> Based on your recent synthetic record</p>
          </div>
          <div className="factor-grid">
            {factors.map((factor) => {
              const Icon = factor.icon;
              return (
                <article className={`factor-card ${factor.tone}`} key={factor.title}>
                  <div className="factor-topline">
                    <div className="factor-icon"><Icon aria-hidden="true" size={28} strokeWidth={2.2} /></div>
                    <span className="factor-status">{factor.status}</span>
                  </div>
                  <h3>{factor.title}</h3>
                  <p className="factor-detail">{factor.detail}</p>
                  <p className="factor-note">{factor.note}</p>
                  <button className="factor-open" type="button" onClick={() => setSelectedFactor(factor)}>
                    View {factor.title.toLowerCase()} <ChevronRight aria-hidden="true" size={21} />
                  </button>
                </article>
              );
            })}
          </div>
        </section>

        <section className="coach-card" aria-labelledby="coach-heading">
          <div className="coach-intro">
            <div className="coach-icon"><Sparkles aria-hidden="true" size={27} /></div>
            <div>
              <p className="eyebrow">Your next gentle step</p>
              <h2 id="coach-heading">Would you like help with sleep?</h2>
              <p>Based on your recent sleep observations, a simple evening routine may help. We can talk through it together.</p>
            </div>
          </div>
          <button className="primary-button" type="button" onClick={() => { sendQuestion('How can I sleep better?'); document.querySelector('#ask')?.scrollIntoView({ behavior: 'smooth' }); }}>
            <MessageCircle aria-hidden="true" size={22} /> Talk about my sleep
          </button>
        </section>

        <section className="care-layout" id="ask" aria-label="Personal guidance and today’s plan">
          <article className="chat-panel" aria-labelledby="chat-heading">
            <div className="panel-heading">
              <div>
                <p className="eyebrow">Plain-language guidance</p>
                <h2 id="chat-heading">Ask MindStep</h2>
              </div>
              <div className="chat-actions">
                <button className="round-action" type="button" onClick={readLatestAnswer} aria-label="Read latest answer aloud" title="Read latest answer aloud">
                  <Volume2 aria-hidden="true" size={21} />
                </button>
                <button className="round-action" type="button" onClick={resetConversation} aria-label="Reset conversation" title="Reset conversation">
                  <RotateCcw aria-hidden="true" size={20} />
                </button>
              </div>
            </div>

            <div className="message-list" aria-live="polite" aria-busy={thinking}>
              {messages.map((message) => (
                <div className={`message-row ${message.role}`} key={message.id}>
                  {message.role === 'assistant' && <span className="message-avatar" aria-hidden="true"><Sparkles size={18} /></span>}
                  <div className="message-bubble"><span className="message-author">{message.role === 'assistant' ? 'MindStep' : 'You'}</span>{message.text}</div>
                </div>
              ))}
              {thinking && (
                <div className="message-row assistant">
                  <span className="message-avatar" aria-hidden="true"><Sparkles size={18} /></span>
                  <div className="message-bubble thinking-message"><span className="message-author">MindStep</span>Putting that into plain language<span className="thinking-dots" aria-hidden="true">…</span></div>
                </div>
              )}
              <div ref={messageEndRef} />
            </div>

            <div className="quick-prompts" aria-label="Suggested questions">
              {quickPrompts.map((prompt) => <button type="button" key={prompt} onClick={() => sendQuestion(prompt)} disabled={thinking}>{prompt}</button>)}
            </div>

            <form className="ask-form" onSubmit={handleSubmit}>
              <label htmlFor="question">Ask a question</label>
              <div className="input-row">
                <input id="question" value={question} onChange={(event) => setQuestion(event.target.value)} placeholder="For example: Why does sleep matter?" autoComplete="off" />
                <button className={`voice-button${listening ? ' listening' : ''}`} type="button" onClick={startVoiceInput} aria-label={listening ? 'Listening for your question' : 'Ask using your voice'} title="Ask using your voice">
                  <Mic aria-hidden="true" size={23} />
                </button>
                <button className="send-button" type="submit" disabled={!question.trim() || thinking}>
                  <Send aria-hidden="true" size={21} /> <span>Send</span>
                </button>
              </div>
            </form>
            <p className="medical-note"><ShieldCheck aria-hidden="true" size={18} />MindStep offers general support, not a diagnosis or emergency advice.</p>
          </article>

          <aside className="side-column">
            <section className="plan-card" aria-labelledby="plan-heading">
              <div className="plan-title-row">
                <span className="plan-icon"><SunMedium aria-hidden="true" size={24} /></span>
                <div><p className="eyebrow">Today</p><h2 id="plan-heading">One small step</h2></div>
              </div>
              <button className={`goal-check${goalDone ? ' done' : ''}`} type="button" onClick={() => { setGoalDone((current) => !current); setAnnouncement(goalDone ? 'Step marked as not done.' : 'Great work. Step marked as done.'); }} aria-pressed={goalDone}>
                <span className="check-box" aria-hidden="true">{goalDone && <Check size={22} strokeWidth={3} />}</span>
                <span><strong>Begin winding down 20 minutes earlier</strong><small>Suggested from your sleep pattern</small></span>
              </button>
              <div className="encouragement">{goalDone ? 'Nice work — small changes count.' : 'Take it at your own pace. You can change this step at any time.'}</div>
            </section>

            <section className="data-card" aria-labelledby="data-heading">
              <div className="data-icon"><Database aria-hidden="true" size={24} /></div>
              <h2 id="data-heading">Where this came from</h2>
              <p>Your suggestions use four standard FHIR resources from a synthetic health record.</p>
              <div className="resource-list" aria-label="FHIR resources used">
                <span>Patient</span><span>Condition</span><span>Observation</span><span>CarePlan</span>
              </div>
              <button className="secondary-button" type="button" onClick={() => setShowProfile(true)}>
                <Eye aria-hidden="true" size={20} /> View data used
              </button>
            </section>

            <section className="help-card">
              <HelpCircle aria-hidden="true" size={24} />
              <div><strong>Not sure what to do?</strong><p>Ask a caregiver or your healthcare professional to review this with you.</p></div>
            </section>
          </aside>
        </section>

        <footer>
          <div className="footer-brand"><span className="brand-mark small"><HeartPulse aria-hidden="true" size={20} /></span><strong>MindStep</strong></div>
          <p>Student prototype for COMP3820 · Team High Five</p>
          <p><LockKeyhole aria-hidden="true" size={16} /> No health information is saved by this front-end demo.</p>
        </footer>
      </main>

      {selectedFactor && (
        <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setSelectedFactor(null); }}>
          <section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="factor-dialog-title">
            <button className="modal-close" type="button" onClick={() => setSelectedFactor(null)} aria-label="Close factor details"><X aria-hidden="true" size={23} /></button>
            <div className={`dialog-factor-icon ${selectedFactor.tone}`}><selectedFactor.icon aria-hidden="true" size={31} /></div>
            <p className="eyebrow">{selectedFactor.status}</p>
            <h2 id="factor-dialog-title">Your {selectedFactor.title.toLowerCase()}</h2>
            <p className="dialog-lead">{selectedFactor.summary}</p>
            <div className="action-callout"><strong>A gentle next step</strong><p>{selectedFactor.action}</p></div>
            <div className="source-row"><Database aria-hidden="true" size={18} /><span>Source: FHIR {selectedFactor.resource} · synthetic record</span></div>
            <button className="primary-button dark" type="button" onClick={() => { setSelectedFactor(null); sendQuestion(`Tell me more about ${selectedFactor.title.toLowerCase()}`); document.querySelector('#ask')?.scrollIntoView({ behavior: 'smooth' }); }}>
              Ask MindStep about this <ChevronRight aria-hidden="true" size={20} />
            </button>
          </section>
        </div>
      )}

      {showAccess && (
        <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setShowAccess(false); }}>
          <section className="modal-card compact" role="dialog" aria-modal="true" aria-labelledby="access-dialog-title">
            <button className="modal-close" type="button" onClick={() => setShowAccess(false)} aria-label="Close display settings"><X aria-hidden="true" size={23} /></button>
            <div className="dialog-heading-icon"><Settings2 aria-hidden="true" size={26} /></div>
            <p className="eyebrow">Accessibility</p>
            <h2 id="access-dialog-title">Make MindStep easier to use</h2>
            <div className="setting-list">
              <button type="button" onClick={() => setLargeText((current) => !current)} aria-pressed={largeText}>
                <span><strong>Larger text</strong><small>Increase text across the page</small></span><span className={`switch ${largeText ? 'on' : ''}`} aria-hidden="true"><span /></span>
              </button>
              <button type="button" onClick={() => setHighContrast((current) => !current)} aria-pressed={highContrast}>
                <span><strong>Higher contrast</strong><small>Use stronger colours and borders</small></span><span className={`switch ${highContrast ? 'on' : ''}`} aria-hidden="true"><span /></span>
              </button>
              <button type="button" onClick={readLatestAnswer}>
                <span><strong>Read latest answer</strong><small>Hear MindStep’s latest reply aloud</small></span><Volume2 aria-hidden="true" size={23} />
              </button>
            </div>
            <p className="keyboard-note">Tip: press the Tab key to move through buttons and links. Press Escape to close this window.</p>
          </section>
        </div>
      )}

      {showProfile && (
        <div className="drawer-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setShowProfile(false); }}>
          <aside className="data-drawer" role="dialog" aria-modal="true" aria-labelledby="record-dialog-title">
            <button className="modal-close" type="button" onClick={() => setShowProfile(false)} aria-label="Close health record"><X aria-hidden="true" size={23} /></button>
            <p className="eyebrow">Synthetic SMART-on-FHIR record</p>
            <h2 id="record-dialog-title">Data used for Margaret</h2>
            <div className="connection-callout"><span className="status-dot" aria-hidden="true" /><span><strong>Connected securely</strong><small>Demo EHR · last read 9:14 am</small></span></div>
            <dl className="record-list">
              <div><dt><ClipboardPlus aria-hidden="true" size={20} /> Patient</dt><dd>Margaret Chen · 68 years · synthetic profile</dd></div>
              <div><dt><HeartPulse aria-hidden="true" size={20} /> Condition</dt><dd>Parkinson’s disease · recorded 2018</dd></div>
              <div><dt><Activity aria-hidden="true" size={20} /> Observation</dt><dd>Sleep duration, daily steps, mobility check</dd></div>
              <div><dt><Brain aria-hidden="true" size={20} /> CarePlan</dt><dd>Movement and cognitive engagement goals</dd></div>
            </dl>
            <div className="privacy-box"><LockKeyhole aria-hidden="true" size={22} /><div><strong>Privacy by design</strong><p>The prototype reads only the data needed for this screen and does not persist protected health information in local storage.</p></div></div>
            <button className="secondary-button full" type="button" onClick={() => setShowProfile(false)}>Done</button>
          </aside>
        </div>
      )}

      <div className="sr-only" aria-live="assertive">{announcement}</div>
    </div>
  );
}
