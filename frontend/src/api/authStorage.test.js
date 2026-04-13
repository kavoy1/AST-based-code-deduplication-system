import test from 'node:test'
import assert from 'node:assert/strict'

import {
  clearAuthSession,
  getAccessToken,
  getStoredUser,
  hasAccessToken,
  saveAuthSession
} from './authStorage.js'

function createMemoryStorage() {
  const data = new Map()
  return {
    getItem(key) {
      return data.has(key) ? data.get(key) : null
    },
    setItem(key, value) {
      data.set(key, String(value))
    },
    removeItem(key) {
      data.delete(key)
    }
  }
}

test('saveAuthSession stores access token and user profile', () => {
  globalThis.localStorage = createMemoryStorage()

  saveAuthSession({
    accessToken: 'access-token-demo',
    user: { id: 3, role: 'TEACHER', username: 'teacher01' },
    latestNotice: { id: 1 }
  })

  assert.equal(hasAccessToken(), true)
  assert.equal(getAccessToken(), 'access-token-demo')
  assert.deepEqual(getStoredUser(), { id: 3, role: 'TEACHER', username: 'teacher01' })
})

test('clearAuthSession removes access token and user artifacts', () => {
  globalThis.localStorage = createMemoryStorage()

  saveAuthSession({
    accessToken: 'access-token-demo',
    user: { id: 3, role: 'TEACHER' },
    latestNotice: { id: 1 }
  })
  clearAuthSession()

  assert.equal(hasAccessToken(), false)
  assert.equal(getAccessToken(), '')
  assert.deepEqual(getStoredUser(), {})
})
