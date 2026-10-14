import React, { useState } from 'react';
import { Fingerprint, UserPlus, LogIn, CheckCircle2, AlertCircle, Loader2 } from 'lucide-react';
import * as WebAuthn from './services/WebAuthnService';

function App() {
  const [isLogin, setIsLogin] = useState(true);
  const [username, setUsername] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [status, setStatus] = useState<{ type: 'success' | 'error' | 'none', message: string }>({ type: 'none', message: '' });
  const [loading, setLoading] = useState(false);
  const [isLoggedIn, setIsLoggedIn] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setStatus({ type: 'none', message: '' });

    try {
      if (isLogin) {
        await WebAuthn.startAuthentication(username);
        setStatus({ type: 'success', message: 'Authentication successful! Welcome back.' });
        setIsLoggedIn(true);
      } else {
        await WebAuthn.startRegistration(username, displayName);
        setStatus({ type: 'success', message: 'Registration successful! You can now log in.' });
        setIsLogin(true);
      }
    } catch (err: any) {
      console.error(err);
      setStatus({ type: 'error', message: err.message || 'An error occurred' });
    } finally {
      setLoading(false);
    }
  };

  if (isLoggedIn) {
    return (
      <div className="app-container">
        <div className="glass-card" style={{ textAlign: 'center' }}>
          <CheckCircle2 size={64} color="#10b981" style={{ margin: '0 auto 1.5rem' }} />
          <h1>Welcome, {username}!</h1>
          <p className="subtitle">You have successfully authenticated using FIDO2/WebAuthn.</p>
          <button onClick={() => setIsLoggedIn(false)} style={{ marginTop: '1rem' }}>
            Logout
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="app-container">
      <div className="background-blobs">
        <div className="blob blob-1"></div>
        <div className="blob blob-2"></div>
      </div>

      <div className="glass-card">
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '1.5rem' }}>
          <div style={{
            background: 'rgba(99, 102, 241, 0.1)',
            padding: '1rem',
            borderRadius: '1rem',
            color: 'var(--primary)'
          }}>
            <Fingerprint size={40} />
          </div>
        </div>

        <h1>{isLogin ? 'Welcome Back' : 'Create Account'}</h1>
        <p className="subtitle">
          {isLogin
            ? 'Sign in securely using your biometric or security key'
            : 'Register your device for passwordless authentication'}
        </p>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="username">Username</label>
            <input
              id="username"
              type="text"
              placeholder="e.g. johndoe"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
            />
          </div>

          {!isLogin && (
            <div className="form-group">
              <label htmlFor="displayName">Display Name</label>
              <input
                id="displayName"
                type="text"
                placeholder="e.g. John Doe"
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                required
              />
            </div>
          )}

          <button type="submit" disabled={loading}>
            {loading ? (
              <Loader2 className="animate-spin" size={20} />
            ) : isLogin ? (
              <>
                <LogIn size={20} /> Sign In with FIDO
              </>
            ) : (
              <>
                <UserPlus size={20} /> Register Device
              </>
            )}
          </button>
        </form>

        {status.type !== 'none' && (
          <div className={`status-msg ${status.type}`}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', justifyContent: 'center' }}>
              {status.type === 'success' ? <CheckCircle2 size={16} /> : <AlertCircle size={16} />}
              {status.message}
            </div>
          </div>
        )}

        <a className="toggle-link" onClick={() => {
          setIsLogin(!isLogin);
          setStatus({ type: 'none', message: '' });
        }}>
          {isLogin ? "Don't have an account? Register" : "Already have an account? Sign In"}
        </a>
      </div>
    </div>
  );
}

export default App;
