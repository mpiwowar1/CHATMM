const DB_NAME = "app-device-store"
const DB_VERSION = 1
const STORE_NAME = "keys"
const DEVICE_KEY_ID = "deviceKey"
const WRAPPED_KEY_LS = "wrappedPrivateKey"
const WRAPPED_IV_LS = "wrappedPrivateKeyIv"

/** Open (and initialize) the device IndexedDB store. */
function openDb(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, DB_VERSION)
    req.onupgradeneeded = () => req.result.createObjectStore(STORE_NAME)
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
  })
}

/** Read a CryptoKey from the IndexedDB store by key. */
function idbGet(db: IDBDatabase, key: string): Promise<CryptoKey | undefined> {
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, "readonly")
    const req = tx.objectStore(STORE_NAME).get(key)
    req.onsuccess = () => resolve(req.result as CryptoKey | undefined)
    req.onerror = () => reject(req.error)
  })
}

/** Store a CryptoKey in IndexedDB under the given key. */
function idbSet(db: IDBDatabase, key: string, value: CryptoKey): Promise<void> {
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, "readwrite")
    const req = tx.objectStore(STORE_NAME).put(value, key)
    req.onsuccess = () => resolve()
    req.onerror = () => reject(req.error)
  })
}

/** Delete an entry from the device IndexedDB store. */
function idbDelete(db: IDBDatabase, key: string): Promise<void> {
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, "readwrite")
    const req = tx.objectStore(STORE_NAME).delete(key)
    req.onsuccess = () => resolve()
    req.onerror = () => reject(req.error)
  })
}

async function getOrCreateDeviceKey(): Promise<CryptoKey> {
  const db = await openDb()
  const existing = await idbGet(db, DEVICE_KEY_ID)
  if (existing) return existing

  const key = await crypto.subtle.generateKey(
    { name: "AES-GCM", length: 256 },
    false,
    ["encrypt", "decrypt"]
  )
  await idbSet(db, DEVICE_KEY_ID, key)
  return key
}

/** Return stored device AES key or null. */
async function getDeviceKey(): Promise<CryptoKey | null> {
  const db = await openDb()
  return (await idbGet(db, DEVICE_KEY_ID)) ?? null
}

import { b64, fromB64 } from "./utils"

/** Convert ArrayBuffer to base64 string. */
// `b64` imported from utils

/** Wrap an RSA private key with the device key and persist in localStorage. */
export async function wrapAndStorePrivateKey(
  privateKey: CryptoKey
): Promise<void> {
  const deviceKey = await getOrCreateDeviceKey()
  const pkcs8 = await crypto.subtle.exportKey("pkcs8", privateKey)
  const iv = crypto.getRandomValues(new Uint8Array(12))
  const wrapped = await crypto.subtle.encrypt(
    { name: "AES-GCM", iv },
    deviceKey,
    pkcs8
  )
  localStorage.setItem(WRAPPED_KEY_LS, b64(wrapped))
  localStorage.setItem(WRAPPED_IV_LS, b64(iv))
}

/** Load and unwrap a previously wrapped private key from localStorage. */
export async function loadWrappedPrivateKey(): Promise<CryptoKey | null> {
  const wrappedB64 = localStorage.getItem(WRAPPED_KEY_LS)
  const ivB64 = localStorage.getItem(WRAPPED_IV_LS)
  if (!wrappedB64 || !ivB64) return null

  const deviceKey = await getDeviceKey()
  if (!deviceKey) return null

  try {
    const pkcs8 = await crypto.subtle.decrypt(
      { name: "AES-GCM", iv: fromB64(ivB64) },
      deviceKey,
      fromB64(wrappedB64)
    )
    return crypto.subtle.importKey(
      "pkcs8",
      pkcs8,
      { name: "RSA-OAEP", hash: "SHA-256" },
      false,
      ["decrypt"]
    )
  } catch {
    clearDeviceKeyStore()
    return null
  }
}

/** Clear device key entries from localStorage and IndexedDB. */
export async function clearDeviceKeyStore(): Promise<void> {
  localStorage.removeItem(WRAPPED_KEY_LS)
  localStorage.removeItem(WRAPPED_IV_LS)
  try {
    const db = await openDb()
    await idbDelete(db, DEVICE_KEY_ID)
  } catch {}
}
