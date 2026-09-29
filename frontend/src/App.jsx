import { useEffect } from 'react'
import { Navigate, Route, Routes, NavLink, useLocation } from 'react-router-dom'
import { useAuth } from './auth'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Logs from './pages/Logs'
import Alerts from './pages/Alerts'
import Incidents from './pages/Incidents'
import Rules from './pages/Rules'
import ThreatIntel from './pages/ThreatIntel'
import Simulator from './pages/Simulator'
import Reports from './pages/Reports'
import Audit from './pages/Audit'
import Users from './pages/Users'

function ShieldIcon({ size = 26 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24">
      <path fill="#22c55e" d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z" />
      <path fill="#06121f" d="M10.6 15.4l-2.9-2.9 1.27-1.27 1.63 1.62 4.13-4.12 1.27 1.28z" />
    </svg>
  )
}

const NAV = [
  { to: '/', label: 'Dashboard', end: true },
  { section: 'Monitoring' },
  { to: '/logs', label: 'Logs' },
  { to: '/alerts', label: 'Alerts' },
  { to: '/incidents', label: 'Incidents' },
  { section: 'Tools' },
  { to: '/simulator', label: 'Simulator' },
  { to: '/threat-intel', label: 'Threat Intel' },
  { to: '/rules', label: 'Detection Rules' },
  { to: '/reports', label: 'Reports' },
]

function Sidebar() {
  const { user, logout } = useAuth()
  return (
    <aside className="sidebar">
      <div className="brand"><ShieldIcon /> Sentinel<span>X</span></div>
      <nav className="nav">
        {NAV.map((item, i) =>
          item.section
            ? <div key={i} className="nav-section">{item.section}</div>
            : <NavLink key={item.to} to={item.to} end={item.end}
                className={({ isActive }) => isActive ? 'active' : ''}>{item.label}</NavLink>
        )}
        {user?.role === 'ADMIN' && (
          <>
            <div className="nav-section">Administration</div>
            <NavLink to="/audit" className={({ isActive }) => isActive ? 'active' : ''}>Audit Log</NavLink>
            <NavLink to="/users" className={({ isActive }) => isActive ? 'active' : ''}>Users</NavLink>
          </>
        )}
      </nav>
      <div style={{ padding: 14, borderTop: '1px solid var(--border)', display: 'flex', alignItems: 'center', gap: 10 }}>
        <div className="avatar">{user?.username?.slice(0, 2)}</div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 13, fontWeight: 600 }}>{user?.username}</div>
          <small className="dim">{user?.role}</small>
        </div>
        <button className="btn sm secondary" onClick={logout}>Exit</button>
      </div>
    </aside>
  )
}

function Page({ title, children }) {
  const location = useLocation()
  useEffect(() => { document.title = `${title} | SentinelX` }, [title])
  return (
    <>
      <header className="topbar">
        <h1>{location.state?.heading || title}</h1>
      </header>
      <div className="content">{children}</div>
    </>
  )
}

export { Page, ShieldIcon }

export default function App() {
  const { user, loading } = useAuth()

  if (loading) return <div className="loading">Loading SentinelX…</div>

  if (!user) {
    return (
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    )
  }

  return (
    <div className="shell">
      <Sidebar />
      <main className="main">
        <Routes>
          <Route path="/" element={<Page title="Dashboard"><Dashboard /></Page>} />
          <Route path="/logs" element={<Page title="Raw Logs"><Logs /></Page>} />
          <Route path="/alerts" element={<Page title="Alerts"><Alerts /></Page>} />
          <Route path="/incidents" element={<Page title="Incidents"><Incidents /></Page>} />
          <Route path="/simulator" element={<Page title="Attack Simulator"><Simulator /></Page>} />
          <Route path="/threat-intel" element={<Page title="Threat Intelligence"><ThreatIntel /></Page>} />
          <Route path="/rules" element={<Page title="Detection Rules"><Rules /></Page>} />
          <Route path="/reports" element={<Page title="Reports"><Reports /></Page>} />
          {user.role === 'ADMIN' && (
            <>
              <Route path="/audit" element={<Page title="Audit Log"><Audit /></Page>} />
              <Route path="/users" element={<Page title="User Management"><Users /></Page>} />
            </>
          )}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  )
}
