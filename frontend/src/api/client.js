import axios from 'axios'

const STORAGE_KEY = 'simple-crm.auth'
const UNAUTHORIZED_EVENT = 'simple-crm:unauthorized'

export const authStorage = {
  read() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      return raw ? JSON.parse(raw) : null
    } catch {
      return null
    }
  },
  write(value) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(value))
  },
  clear() {
    localStorage.removeItem(STORAGE_KEY)
  },
}

export const api = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

api.interceptors.request.use((config) => {
  const auth = authStorage.read()
  if (auth?.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status
    const url = error?.config?.url ?? ''
    if (status === 401 && !url.includes('/auth/login')) {
      authStorage.clear()
      window.dispatchEvent(new Event(UNAUTHORIZED_EVENT))
    }
    return Promise.reject(error)
  },
)

export function onUnauthorized(handler) {
  window.addEventListener(UNAUTHORIZED_EVENT, handler)
  return () => window.removeEventListener(UNAUTHORIZED_EVENT, handler)
}

export function fieldErrorsOf(error) {
  return error?.response?.data?.fieldErrors ?? null
}

export function errorMessageOf(error) {
  const data = error?.response?.data
  if (data?.fieldErrors && Object.keys(data.fieldErrors).length > 0) {
    return Object.entries(data.fieldErrors)
      .map(([field, message]) => `${field}: ${message}`)
      .join(' · ')
  }
  if (data?.message) return data.message
  if (error?.code === 'ECONNABORTED') return 'The request timed out. Please try again.'
  return error?.message ?? 'Something went wrong'
}
