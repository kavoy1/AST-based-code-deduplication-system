const ACCESS_TOKEN_KEY = 'accessToken'
const USER_KEY = 'user'
const NOTICE_KEY = 'latestNotice'
const LEGACY_TOKEN_KEY = 'satoken'

function getStorage() {
  if (typeof localStorage === 'undefined') {
    return null
  }
  return localStorage
}

export function getAccessToken() {
  const storage = getStorage()
  if (!storage) return ''
  return storage.getItem(ACCESS_TOKEN_KEY) || storage.getItem(LEGACY_TOKEN_KEY) || ''
}

export function hasAccessToken() {
  return Boolean(getAccessToken())
}

export function setAccessToken(accessToken) {
  const storage = getStorage()
  if (!storage) return
  if (accessToken) {
    storage.setItem(ACCESS_TOKEN_KEY, accessToken)
    storage.removeItem(LEGACY_TOKEN_KEY)
    return
  }
  storage.removeItem(ACCESS_TOKEN_KEY)
  storage.removeItem(LEGACY_TOKEN_KEY)
}

export function getStoredUser() {
  const storage = getStorage()
  if (!storage) return {}
  try {
    return JSON.parse(storage.getItem(USER_KEY) || '{}')
  } catch {
    return {}
  }
}

export function saveAuthSession({ accessToken, user, latestNotice } = {}) {
  const storage = getStorage()
  if (!storage) return
  setAccessToken(accessToken || '')
  if (user !== undefined) {
    storage.setItem(USER_KEY, JSON.stringify(user || {}))
  }
  if (latestNotice !== undefined) {
    storage.setItem(NOTICE_KEY, JSON.stringify(latestNotice || null))
  }
}

export function clearAuthSession() {
  const storage = getStorage()
  if (!storage) return
  storage.removeItem(ACCESS_TOKEN_KEY)
  storage.removeItem(LEGACY_TOKEN_KEY)
  storage.removeItem(USER_KEY)
  storage.removeItem(NOTICE_KEY)
}
