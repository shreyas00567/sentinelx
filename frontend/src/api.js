const TOKEN_KEY = 'sentinelx_token'

const API_BASE = (import.meta.env.VITE_API_URL || '').replace(/\/+$/, '')

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  if (token) localStorage.setItem(TOKEN_KEY, token)
  else localStorage.removeItem(TOKEN_KEY)
}

export async function api(path, { method = 'GET', body, params } = {}) {
  const url = new URL(path, window.location.origin)
  if (params) {
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') url.searchParams.set(k, v)
    })
  }
  const headers = {}
  if (body) headers['Content-Type'] = 'application/json'
  const token = getToken()
  if (token) headers['Authorization'] = `Bearer ${token}`

  const res = await fetch(API_BASE + url.pathname + url.search, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined
  })

  if (res.status === 401 && getToken()) {
    setToken(null)
    window.location.href = '/login'
    throw new Error('Session expired')
  }

  let data = null
  const text = await res.text()
  if (text) {
    try { data = JSON.parse(text) } catch { data = text }
  }
  if (!res.ok) {
    const detail = data && typeof data === 'object'
      ? (data.detail || data.message || data.error || JSON.stringify(data))
      : (data || res.statusText)
    throw new Error(detail)
  }
  return data
}
