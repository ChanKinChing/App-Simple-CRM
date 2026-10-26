import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, errorMessageOf, fieldErrorsOf } from '../api/client'
import { useAuth } from '../auth/useAuth'
import CustomerForm from '../components/CustomerForm'
import Icon from '../components/Icon'
import Modal from '../components/Modal'
import Pagination from '../components/Pagination'

const PAGE_SIZE = 10

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleDateString('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

export default function CustomersPage() {
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'

  const [query, setQuery] = useState('')
  const [debounced, setDebounced] = useState('')
  const [page, setPage] = useState(0)
  const [reloadKey, setReloadKey] = useState(0)

  const [data, setData] = useState({ content: [], page: 0, totalElements: 0, totalPages: 0 })
  const [summary, setSummary] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [notice, setNotice] = useState(null)

  const [form, setForm] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const [formError, setFormError] = useState(null)
  const [serverErrors, setServerErrors] = useState(null)

  const [confirm, setConfirm] = useState(null)
  const [deleting, setDeleting] = useState(false)

  useEffect(() => {
    const timer = setTimeout(() => {
      const next = query.trim()
      if (next !== debounced) {
        setDebounced(next)
        setPage(0)
      }
    }, 300)
    return () => clearTimeout(timer)
  }, [query, debounced])

  useEffect(() => {
    let active = true
    setLoading(true)
    setError(null)
    api
      .get('/customers', {
        params: { q: debounced || undefined, page, size: PAGE_SIZE, sort: 'createdAt,desc' },
      })
      .then((response) => {
        if (active) setData(response.data)
      })
      .catch((err) => {
        if (active) setError(errorMessageOf(err))
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [debounced, page, reloadKey])

  useEffect(() => {
    let active = true
    api
      .get('/reports/summary')
      .then((response) => {
        if (active) setSummary(response.data)
      })
      .catch(() => {
        if (active) setSummary(null)
      })
    return () => {
      active = false
    }
  }, [reloadKey])

  const refresh = (message) => {
    if (message) setNotice(message)
    setReloadKey((value) => value + 1)
  }

  const openCreate = () => {
    setFormError(null)
    setServerErrors(null)
    setForm({ mode: 'create', customer: null })
  }

  const openEdit = (customer) => {
    setFormError(null)
    setServerErrors(null)
    setForm({ mode: 'edit', customer })
  }

  const closeForm = () => {
    if (submitting) return
    setForm(null)
  }

  const handleSave = async (values) => {
    setSubmitting(true)
    setFormError(null)
    setServerErrors(null)
    try {
      if (form.mode === 'create') {
        await api.post('/customers', values)
      } else {
        await api.put(`/customers/${form.customer.id}`, values)
      }
      setForm(null)
      refresh(form.mode === 'create' ? 'Customer created' : 'Customer updated')
    } catch (err) {
      const fields = fieldErrorsOf(err)
      if (fields) {
        setServerErrors(fields)
      } else {
        setFormError(errorMessageOf(err))
      }
    } finally {
      setSubmitting(false)
    }
  }

  const handleDelete = async () => {
    setDeleting(true)
    setFormError(null)
    try {
      await api.delete(`/customers/${confirm.id}`)
      setConfirm(null)
      refresh('Customer deleted')
    } catch (err) {
      setFormError(errorMessageOf(err))
      setConfirm(null)
    } finally {
      setDeleting(false)
    }
  }

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Customers</h1>
          <p className="subtle">Search, review and maintain the customer book.</p>
        </div>
        <button type="button" className="btn btn-filled" onClick={openCreate}>
          <Icon name="add" size={18} />
          New customer
        </button>
      </div>

      <div className="stat-grid">
        <div className="stat">
          <span className="stat-label">Total customers</span>
          <span className="stat-value">{summary?.totalCustomers ?? '-'}</span>
        </div>
        <div className="stat">
          <span className="stat-label">New this month</span>
          <span className="stat-value">{summary?.newCustomersThisMonth ?? '-'}</span>
        </div>
        <div className="stat">
          <span className="stat-label">Follow-ups this month</span>
          <span className="stat-value">{summary?.followUpsThisMonth ?? '-'}</span>
        </div>
      </div>

      {notice ? (
        <div className="alert alert-success" role="status" style={{ marginBottom: 16 }}>
          <Icon name="check" size={18} />
          <span style={{ flex: 1 }}>{notice}</span>
          <button type="button" className="btn btn-icon btn-sm" onClick={() => setNotice(null)} aria-label="Dismiss">
            <Icon name="close" size={16} />
          </button>
        </div>
      ) : null}

      {error ? (
        <div className="alert alert-error" role="alert" style={{ marginBottom: 16 }}>
          <Icon name="close" size={18} />
          <span>{error}</span>
        </div>
      ) : null}

      <div className="toolbar">
        <div className="search">
          <Icon name="search" size={20} />
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Search name, company, email or phone"
            aria-label="Search customers"
          />
        </div>
        {debounced ? (
          <button type="button" className="btn btn-sm" onClick={() => setQuery('')}>
            Clear search
          </button>
        ) : null}
      </div>

      <div className="table-wrap">
        {loading ? (
          <div className="loading">Loading customers...</div>
        ) : data.content.length === 0 ? (
          <div className="empty">
            <p style={{ marginBottom: 8 }}>No customers found.</p>
            <p className="subtle">
              {debounced ? 'Try a different keyword.' : 'Create the first customer to get started.'}
            </p>
          </div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Company</th>
                <th>Email</th>
                <th>Phone</th>
                <th>Created</th>
                <th style={{ textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((customer) => (
                <tr key={customer.id}>
                  <td>
                    <Link to={`/customers/${customer.id}`} className="cell-strong">
                      {customer.name}
                    </Link>
                  </td>
                  <td className="cell-muted">{customer.company || '-'}</td>
                  <td className="cell-muted">{customer.email || '-'}</td>
                  <td className="cell-muted">{customer.phone || '-'}</td>
                  <td className="cell-muted">{formatDate(customer.createdAt)}</td>
                  <td>
                    <div className="row-actions">
                      <button
                        type="button"
                        className="btn btn-icon"
                        onClick={() => openEdit(customer)}
                        aria-label={`Edit ${customer.name}`}
                        title="Edit"
                      >
                        <Icon name="edit" size={18} />
                      </button>
                      {isAdmin ? (
                        <button
                          type="button"
                          className="btn btn-icon btn-danger"
                          onClick={() => setConfirm(customer)}
                          aria-label={`Delete ${customer.name}`}
                          title="Delete"
                        >
                          <Icon name="delete" size={18} />
                        </button>
                      ) : null}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        <Pagination
          page={data.page}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          onPageChange={setPage}
          disabled={loading}
        />
      </div>

      {form ? (
        <Modal
          title={form.mode === 'create' ? 'New customer' : 'Edit customer'}
          subtitle={form.mode === 'create' ? 'Add a contact to the customer book.' : form.customer.name}
          onClose={closeForm}
        >
          {formError ? (
            <div className="alert alert-error" role="alert" style={{ marginBottom: 16 }}>
              <Icon name="close" size={18} />
              <span>{formError}</span>
            </div>
          ) : null}
          <CustomerForm
            initial={form.customer}
            submitting={submitting}
            serverErrors={serverErrors}
            onSubmit={handleSave}
            onCancel={closeForm}
          />
        </Modal>
      ) : null}

      {confirm ? (
        <Modal
          title="Delete customer"
          subtitle={`This will also remove every follow-up of ${confirm.name}.`}
          onClose={() => setConfirm(null)}
          footer={
            <>
              <button type="button" className="btn" onClick={() => setConfirm(null)} disabled={deleting}>
                Cancel
              </button>
              <button type="button" className="btn btn-filled" onClick={handleDelete} disabled={deleting}>
                {deleting ? 'Deleting...' : 'Delete'}
                <Icon name="delete" size={18} />
              </button>
            </>
          }
        >
          <p className="subtle">This action cannot be undone.</p>
        </Modal>
      ) : null}
    </>
  )
}
