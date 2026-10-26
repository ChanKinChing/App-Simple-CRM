import { useState } from 'react'
import Icon from './Icon'

const EMPTY = { name: '', company: '', email: '', phone: '', note: '' }

export default function CustomerForm({ initial, submitting, serverErrors, onSubmit, onCancel }) {
  const [values, setValues] = useState(() => ({ ...EMPTY, ...(initial ?? {}) }))
  const [localErrors, setLocalErrors] = useState({})

  const setField = (field) => (event) => {
    setValues((current) => ({ ...current, [field]: event.target.value }))
    setLocalErrors((current) => ({ ...current, [field]: undefined }))
  }

  const handleSubmit = (event) => {
    event.preventDefault()
    const errors = {}
    if (!values.name.trim()) errors.name = 'Name is required'
    if (values.email.trim() && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(values.email.trim())) {
      errors.email = 'Enter a valid email address'
    }
    if (Object.keys(errors).length > 0) {
      setLocalErrors(errors)
      return
    }
    onSubmit({
      name: values.name.trim(),
      company: values.company.trim(),
      email: values.email.trim(),
      phone: values.phone.trim(),
      note: values.note.trim(),
    })
  }

  const errorFor = (field) => localErrors[field] ?? serverErrors?.[field]

  return (
    <form onSubmit={handleSubmit} noValidate>
      <div className="form-grid">
        <div className={`field ${errorFor('name') ? 'invalid' : ''}`}>
          <label htmlFor="customer-name">Name *</label>
          <input
            id="customer-name"
            value={values.name}
            onChange={setField('name')}
            maxLength={100}
            autoFocus
            placeholder="Chan Ka Ming"
          />
          {errorFor('name') ? <span className="field-error">{errorFor('name')}</span> : null}
        </div>

        <div className={`field ${errorFor('company') ? 'invalid' : ''}`}>
          <label htmlFor="customer-company">Company</label>
          <input
            id="customer-company"
            value={values.company}
            onChange={setField('company')}
            maxLength={120}
            placeholder="Golden Harbour Trading Ltd"
          />
          {errorFor('company') ? <span className="field-error">{errorFor('company')}</span> : null}
        </div>

        <div className={`field ${errorFor('email') ? 'invalid' : ''}`}>
          <label htmlFor="customer-email">Email</label>
          <input
            id="customer-email"
            type="email"
            value={values.email}
            onChange={setField('email')}
            maxLength={120}
            placeholder="name@example.com"
          />
          {errorFor('email') ? <span className="field-error">{errorFor('email')}</span> : null}
        </div>

        <div className={`field ${errorFor('phone') ? 'invalid' : ''}`}>
          <label htmlFor="customer-phone">Phone</label>
          <input
            id="customer-phone"
            value={values.phone}
            onChange={setField('phone')}
            maxLength={30}
            placeholder="+852 9000 0000"
          />
          {errorFor('phone') ? <span className="field-error">{errorFor('phone')}</span> : null}
        </div>

        <div className={`field full ${errorFor('note') ? 'invalid' : ''}`}>
          <label htmlFor="customer-note">Note</label>
          <textarea
            id="customer-note"
            value={values.note}
            onChange={setField('note')}
            maxLength={1000}
            placeholder="Context, preferences, next steps"
          />
          {errorFor('note') ? <span className="field-error">{errorFor('note')}</span> : null}
        </div>
      </div>

      <div className="modal-actions">
        <button type="button" className="btn" onClick={onCancel} disabled={submitting}>
          Cancel
        </button>
        <button type="submit" className="btn btn-filled" disabled={submitting}>
          {submitting ? 'Saving...' : 'Save'}
          <Icon name="check" size={18} />
        </button>
      </div>
    </form>
  )
}
