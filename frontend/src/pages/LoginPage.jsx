import { useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { errorMessageOf } from '../api/client'
import Icon from '../components/Icon'

export default function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [username, setUsername] = useState('admin')
  const [password, setPassword] = useState('Passw0rd!')
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  if (user) {
    const target = location.state?.from?.pathname ?? '/customers'
    return <Navigate to={target} replace />
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await login(username.trim(), password)
      navigate(location.state?.from?.pathname ?? '/customers', { replace: true })
    } catch (err) {
      setError(errorMessageOf(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-brand">
          <span className="brand-mark">
            <Icon name="building" size={24} />
          </span>
          <div>
            <h1>Simple CRM</h1>
            <p className="subtle">Customer management for small teams</p>
          </div>
        </div>

        <form className="login-form" onSubmit={handleSubmit}>
          {error ? (
            <div className="alert alert-error" role="alert">
              <Icon name="close" size={18} />
              <span>{error}</span>
            </div>
          ) : null}

          <div className="field">
            <label htmlFor="username">Username</label>
            <input
              id="username"
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              autoComplete="username"
              required
            />
          </div>

          <div className="field">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              autoComplete="current-password"
              required
            />
          </div>

          <button type="submit" className="btn btn-filled" disabled={submitting}>
            {submitting ? 'Signing in...' : 'Sign in'}
          </button>
        </form>

        <div className="hint">
          Demo accounts (local seed data):
          <br />
          <strong>admin</strong> / Passw0rd! &mdash; full access, can delete customers
          <br />
          <strong>sales</strong> / Passw0rd! &mdash; sales role, cannot delete customers
        </div>
      </div>
    </div>
  )
}
