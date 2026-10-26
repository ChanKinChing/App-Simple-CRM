import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { useAuth } from './auth/useAuth'
import AppShell from './layout/AppShell'
import CustomerDetailPage from './pages/CustomerDetailPage'
import CustomersPage from './pages/CustomersPage'
import LoginPage from './pages/LoginPage'

function RequireAuth({ children }) {
  const { user } = useAuth()
  const location = useLocation()
  if (!user) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }
  return children
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        element={
          <RequireAuth>
            <AppShell />
          </RequireAuth>
        }
      >
        <Route path="/" element={<Navigate to="/customers" replace />} />
        <Route path="/customers" element={<CustomersPage />} />
        <Route path="/customers/:id" element={<CustomerDetailPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/customers" replace />} />
    </Routes>
  )
}
