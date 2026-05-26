const subtle = window.crypto.subtle

const b64 = (buf: ArrayBuffer | Uint8Array): string => {
  const bytes = buf instanceof Uint8Array ? buf : new Uint8Array(buf)
  return btoa(String.fromCharCode(...bytes))
}

/** Generate a fresh AES-256-GCM key for a conversation */
export async function generateConversationKey(): Promise<CryptoKey> {
  return subtle.generateKey(
    { name: "AES-GCM", length: 256 },
    true,
    ["encrypt", "decrypt"]
  )
}

/**
 * Encrypt the AES conversation key with a participant's RSA public key.
 * publicKeyB64 is the base64-encoded SPKI bytes stored in the backend.
 * Returns base64-encoded ciphertext to store as encryptedAesKey.
 */
export async function encryptKeyForParticipant(
  aesKey: CryptoKey,
  publicKeyB64: string
): Promise<string> {
  const spkiBytes = Uint8Array.from(atob(publicKeyB64), (c) => c.charCodeAt(0))

  const rsaPublicKey = await subtle.importKey(
    "spki",
    spkiBytes,
    { name: "RSA-OAEP", hash: "SHA-256" },
    false,
    ["encrypt"]
  )

  const rawAes = await subtle.exportKey("raw", aesKey)

  const encrypted = await subtle.encrypt(
    { name: "RSA-OAEP" },
    rsaPublicKey,
    rawAes
  )

  return b64(encrypted)
}

export type ParticipantKey = {
  id: number
  publicKey: string
}

/**
 * Build the participantKeys map the backend expects:
 * { [userId]: base64(RSA-OAEP encrypted AES key) }
 */
export async function buildParticipantKeysMap(
  participants: ParticipantKey[]
): Promise<Record<number, string>> {
  const aesKey = await generateConversationKey()

  const entries = await Promise.all(
    participants.map(async ({ id, publicKey }) => {
      const encryptedKey = await encryptKeyForParticipant(aesKey, publicKey)
      return [id, encryptedKey] as [number, string]
    })
  )

  return Object.fromEntries(entries)
}