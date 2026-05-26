/**
 * In-memory store for the session RSA private key and per-conversation AES keys.
 * Nothing is persisted — cleared on page refresh, forcing re-login.
 */

let _privateKey: CryptoKey | null = null
const _conversationKeys = new Map<number, CryptoKey>()

export function storePrivateKey(key: CryptoKey): void {
  _privateKey = key
}

export function getPrivateKey(): CryptoKey | null {
  return _privateKey
}

export function clearPrivateKey(): void {
  _privateKey = null
  _conversationKeys.clear()
}

export function cacheConversationKey(conversationId: number, key: CryptoKey): void {
  _conversationKeys.set(conversationId, key)
}

export function getCachedConversationKey(conversationId: number): CryptoKey | null {
  return _conversationKeys.get(conversationId) ?? null
}