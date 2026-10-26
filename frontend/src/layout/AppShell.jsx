import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import Icon from '../components/Icon'

export default function AppShell() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const initials = (user?.displayName ?? user?.username ?? '?')
    .split(' ')
    .map((part) => part[0])
    .join('')
    .slice(0, 2)
    .toUpperCase()

  const handleLogout = () => {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">
          <span className="brand-mark">
            <Icon name="building" size={20} />
          </span>
          Simple CRM
        </div>

        <nav aria-label="Main navigation">
          <NavLink to="/customers" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
            Customers
          </NavLink>
        </nav>

        <span className="topbar-spacer" />

        <div className="user-chip">
          <span className="avatar" aria-hidden="true">
            {initials}
          </span>
          <span className="user-meta">
            <strong>{user?.displayName ?? user?.username}</strong>
            <span>{user?.role}</span>
          </span>
        </div>

        <button type="button" className="btn btn-icon" onClick={handleLogout} aria-label="Sign out" title="Sign out">
          <Icon name="logout" />
        </button>
      </header>

      <main className="page">
        <Outlet />
      </main>
    </div>
  )
}
