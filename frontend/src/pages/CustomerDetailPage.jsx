import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, errorMessageOf, fieldErrorsOf } from '../api/client'
import CustomerForm from '../components/CustomerForm'
import Icon from '../components/Icon'
import Modal from '../components/Modal'

function formatDateTime(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export default function CustomerDetailPage() {
  const { id } = useParams()

  const [customer, setCustomer] = useState(null)
  const [followUps, setFollowUps] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const [editing, setEditing] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [formError, setFormError] = useState(null)
  const [serverErrors, setServerErrors] = useState(null)

  const [note, setNote] = useState('')
  const [addingNote, setAddingNote] = useState(false)
  const [noteError, setNoteError] = useState(null)

  const loadFollowUps = useCallback(async () => {
    const response = await api.get(`/customers/${id}/follow-ups`, { params: { size: 100 } })
    setFollowUps(response.data.content)
  }, [id])

  useEffect(() => {
    let active = true
    setLoading(true)
    setError(null)

    api
      .get(`/customers/${id}`)
      .then((response) => {
        if (active) setCustomer(response.data)
        return api.get(`/customers/${id}/follow-ups`, { params: { size: 100 } })
      })
      .then((response) => {
        if (active) setFollowUps(response.data.content)
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
  }, [id])

  const handleSave = async (values) => {
    setSubmitting(true)
    setFormError(null)
    setServerErrors(null)
    try {
      const response = await api.put(`/customers/${id}`, values)
      setCustomer(response.data)
      setEditing(false)
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

  const handleAddNote = async (event) => {
    event.preventDefault()
    if (!note.trim()) {
      setNoteError('Follow-up content is required')
      return
    }
    setAddingNote(true)
    setNoteError(null)
    try {
      await api.post(`/customers/${id}/follow-ups`, { content: note.trim() })
      setNote('')
      await loadFollowUps()
    } catch (err) {
      setNoteError(errorMessageOf(err))
    } finally {
      setAddingNote(false)
    }
  }

  if (loading) {
    return <div className="loading">Loading customer...</div>
  }

  if (error) {
    return (
      <>
        <Link to="/customers" className="back-link">
          <Icon name="back" size={18} />
          Back to customers
        </Link>
        <div className="alert alert-error" role="alert">
          <Icon name="close" size={18} />
          <span>{error}</span>
        </div>
      </>
    )
  }

  if (!customer) {
    return null
  }

  return (
    <>
      <Link to="/customers" className="back-link">
        <Icon name="back" size={18} />
        Back to customers
      </Link>

      <div className="page-head">
        <div>
          <h1>{customer.name}</h1>
          <p className="subtle">{customer.company || 'No company recorded'}</p>
        </div>
        <button
          type="button"
          className="btn btn-tonal"
          onClick={() => {
            setFormError(null)
            setServerErrors(null)
            setEditing(true)
          }}
        >
          <Icon name="edit" size={18} />
          Edit customer
        </button>
      </div>

      <div className="detail-grid">
        <section className="card" aria-labelledby="profile-heading">
          <h2 id="profile-heading" className="stat-label" style={{ marginBottom: 16 }}>
            Profile
          </h2>
          <dl className="info-list">
            <dt>Phone</dt>
            <dd>{customer.phone || '-'}</dd>
            <dt>Email</dt>
            <dd>{customer.email || '-'}</dd>
            <dt>Company</dt>
            <dd>{customer.company || '-'}</dd>
            <dt>Note</dt>
            <dd>{customer.note || '-'}</dd>
            <dt>Created by</dt>
            <dd>{customer.createdBy || '-'}</dd>
            <dt>Created</dt>
            <dd>{formatDateTime(customer.createdAt)}</dd>
            <dt>Updated</dt>
            <dd>{formatDateTime(customer.updatedAt)}</dd>
          </dl>
        </section>

        <section className="card" aria-labelledby="followup-heading">
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 16 }}>
            <Icon name="history" size={18} />
            <h2 id="followup-heading" className="stat-label" style={{ margin: 0 }}>
              Follow-ups
            </h2>
            <span className="badge">{followUps.length}</span>
          </div>

          <form onSubmit={handleAddNote} style={{ marginBottom: 20 }}>
            <div className={`field ${noteError ? 'invalid' : ''}`}>
              <label htmlFor="followup-content">Add a follow-up</label>
              <textarea
                id="followup-content"
                value={note}
                onChange={(event) => {
                  setNote(event.target.value)
                  setNoteError(null)
                }}
                maxLength={2000}
                placeholder="What happened in this interaction?"
              />
              {noteError ? <span className="field-error">{noteError}</span> : null}
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 12 }}>
              <button type="submit" className="btn btn-filled btn-sm" disabled={addingNote}>
                {addingNote ? 'Saving...' : 'Add follow-up'}
                <Icon name="add" size={16} />
              </button>
            </div>
          </form>

          {followUps.length === 0 ? (
            <div className="empty" style={{ padding: '24px 0' }}>
              No follow-ups recorded yet.
            </div>
          ) : (
            <div className="timeline">
              {followUps.map((item) => (
                <article className="timeline-item" key={item.id}>
                  <span className="timeline-dot" aria-hidden="true" />
                  <div className="timeline-body">
                    <p>{item.content}</p>
                    <p className="timeline-meta">
                      {item.createdBy || 'system'} · {formatDateTime(item.createdAt)}
                    </p>
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>
      </div>

      {editing ? (
        <Modal title="Edit customer" subtitle={customer.name} onClose={() => (submitting ? null : setEditing(false))}>
          {formError ? (
            <div className="alert alert-error" role="alert" style={{ marginBottom: 16 }}>
              <Icon name="close" size={18} />
              <span>{formError}</span>
            </div>
          ) : null}
          <CustomerForm
            initial={customer}
            submitting={submitting}
            serverErrors={serverErrors}
            onSubmit={handleSave}
            onCancel={() => setEditing(false)}
          />
        </Modal>
      ) : null}
    </>
  )
}
