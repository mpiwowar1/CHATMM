const subtle = window.crypto.subtle
import { b64 } from "./utils"

/** Generate a random 16-byte front salt. */
export function generateSalt(): Uint8Array {
  return crypto.getRandomValues(new Uint8Array(16))
}

/** Derive a symmetric AES-GCM key from a password and salt. */
export async function deriveKeyFromPassword(
  password: string,
  salt: Uint8Array
): Promise<CryptoKey> {
  const enc = new TextEncoder().encode(password)

  const baseKey = await subtle.importKey("raw", enc, "PBKDF2", false, [
    "deriveKey",
  ])

  return subtle.deriveKey(
    {
      name: "PBKDF2",
      salt: salt,
      iterations: 310_000,
      hash: "SHA-256",
    },
    baseKey,
    {
      name: "AES-GCM",
      length: 256,
    },
    true,
    ["encrypt", "decrypt"]
  )
}

/** Generate an RSA-OAEP key pair for the user. */
export async function generateRSAKeyPair(): Promise<CryptoKeyPair> {
  return subtle.generateKey(
    {
      name: "RSA-OAEP",
      modulusLength: 2048,
      publicExponent: new Uint8Array([1, 0, 1]),
      hash: "SHA-256",
    },
    true,
    ["encrypt", "decrypt"]
  ) as Promise<CryptoKeyPair>
}

/** Encrypt an RSA private key with a wrapping AES key and return base64(iv||ciphertext). */
export async function encryptPrivateKey(
  privateKey: CryptoKey,
  wrappingKey: CryptoKey
): Promise<string> {
  const pkcs8 = await subtle.exportKey("pkcs8", privateKey)

  const iv = crypto.getRandomValues(
    new Uint8Array(12)
  ) as Uint8Array<ArrayBuffer>

  const ciphertext = await subtle.encrypt(
    { name: "AES-GCM", iv },
    wrappingKey,
    pkcs8
  )

  const out = new Uint8Array(iv.byteLength + ciphertext.byteLength)
  out.set(iv, 0)
  out.set(new Uint8Array(ciphertext), iv.byteLength)

  return b64(out)
}

export interface RegistrationPayload {
  name: string
  email: string
  password: string
  frontSalt: string
  publicKey: string
  encryptedPrivateKey: string
}

/** Build the registration payload containing public key and encrypted private key. */
export async function buildRegistrationPayload(
  name: string,
  email: string,
  password: string
): Promise<RegistrationPayload> {
  const salt = await generateSalt()
  const wrappingKey = await deriveKeyFromPassword(password, salt)

  const { publicKey, privateKey } = await generateRSAKeyPair()

  const spki = await subtle.exportKey("spki", publicKey)

  return {
    email,
    name,
    password,
    frontSalt: b64(salt),
    publicKey: b64(spki),
    encryptedPrivateKey: await encryptPrivateKey(privateKey, wrappingKey),
  }
}
