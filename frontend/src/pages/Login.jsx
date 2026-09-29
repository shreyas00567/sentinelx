import { useState } from 'react'
import { useAuth } from '../auth'

export default function Login() {
  const { login } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  async function onSubmit(e) {
    e.preventDefault()
    setError(null)
    setBusy(true)
    try { await login(username.trim(), password) }
    catch (ex) { setError(ex.message) }
    finally { setBusy(false) }
  }

  return (
    <div className="login-wrap">
      <div className="card login-card">
        <div className="login-logo">
          <svg width="40" height="40" viewBox="0 0 24 24">
            <path fill="#22c55e" d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z" />
            <path fill="#06121f" d="M10.6 15.4l-2.9-2.9 1.27-1.27 1.63 1.62 4.13-4.12 1.27 1.28z" />
          </svg>
          <h2 style={{ fontSize: 22 }}>Sentinel<span style={{ color: 'var(--green)' }}>X</span></h2>
        </div>
        <p className="login-sub">Security Operations &amp; Threat Detection Platform</p>

        <form onSubmit={onSubmit}>
          {error && <div className="error-box">{error}</div>}
          <input placeholder="Username" value={username} autoFocus
            onChange={e => setUsername(e.target.value)} />
          <input type="password" placeholder="Password" value={password}
            onChange={e => setPassword(e.target.value)} />
          <button className="btn" disabled={busy || !username || !password}>
            {busy ? 'Signing in…' : 'Sign In'}
          </button>
        </form>

      </div>
    </div>
  )
}
