const subtle = window.crypto.subtle

const b64 = (buf: ArrayBuffer | Uint8Array): string => {
  const bytes = buf instanceof Uint8Array ? buf : new Uint8Array(buf)
  return btoa(String.fromCharCode(...bytes))
}

const fromB64 = (s: string): Uint8Array =>
  Uint8Array.from(atob(s), (c) => c.charCodeAt(0))

/**
 * Decrypt the user's stored private key.
 * Format: base64(12-byte IV || AES-GCM ciphertext of PKCS8)
 */
export async function decryptPrivateKey(
  encryptedPrivateKeyB64: string,
  wrappingKey: CryptoKey
): Promise<CryptoKey> {
  const bytes = fromB64(encryptedPrivateKeyB64)
  const iv = bytes.slice(0, 12)
  const ciphertext = bytes.slice(12)

  const pkcs8 = await subtle.decrypt({ name: "AES-GCM", iv }, wrappingKey, ciphertext)

  return subtle.importKey(
    "pkcs8",
    pkcs8,
    { name: "RSA-OAEP", hash: "SHA-256" },
    false,
    ["decrypt"]
  )
}

/**
 * Decrypt the per-conversation AES key using the user's RSA private key.
 * encryptedAesKeyB64 is what the backend stores as Participant.encryptedAesKey.
 */
export async function decryptConversationKey(
  encryptedAesKeyB64: string,
  privateKey: CryptoKey
): Promise<CryptoKey> {
  const encrypted = fromB64(encryptedAesKeyB64)

  const rawAes = await subtle.decrypt({ name: "RSA-OAEP" }, privateKey, encrypted)

  return subtle.importKey(
    "raw",
    rawAes,
    { name: "AES-GCM", length: 256 },
    false,
    ["encrypt", "decrypt"]
  )
}

/** Encrypt a plaintext string with the conversation AES key */
export async function encryptMessage(
  plaintext: string,
  aesKey: CryptoKey
): Promise<{ ciphertext: string; iv: string }> {
  const iv = crypto.getRandomValues(new Uint8Array(12))
  const encoded = new TextEncoder().encode(plaintext)

  const ciphertextBuf = await subtle.encrypt({ name: "AES-GCM", iv }, aesKey, encoded)

  return { ciphertext: b64(ciphertextBuf), iv: b64(iv) }
}

/** Decrypt a ciphertext+iv pair with the conversation AES key */
export async function decryptMessage(
  ciphertextB64: string,
  ivB64: string,
  aesKey: CryptoKey
): Promise<string> {
  const ciphertext = fromB64(ciphertextB64)
  const iv = fromB64(ivB64)

  const plaintext = await subtle.decrypt({ name: "AES-GCM", iv }, aesKey, ciphertext)

  return new TextDecoder().decode(plaintext)
}