const subtle = window.crypto.subtle

const b64 = (buf: ArrayBuffer | Uint8Array): string => {
  const bytes = buf instanceof Uint8Array ? buf : new Uint8Array(buf)
  return btoa(String.fromCharCode(...bytes))
}

export function generateSalt(): Uint8Array<ArrayBuffer> {
  return crypto.getRandomValues(new Uint8Array(16)) as Uint8Array<ArrayBuffer>
}

export async function deriveKeyFromPassword(
  password: string,
  salt: Uint8Array<ArrayBuffer>
): Promise<CryptoKey> {
  const enc = new TextEncoder().encode(password)

  const baseKey = await subtle.importKey("raw", enc, "PBKDF2", false, [
    "deriveKey",
  ])

  return subtle.deriveKey(
    {
      name: "PBKDF2",
      salt,
      iterations: 310_000,
      hash: "SHA-256",
    },
    baseKey,
    {
      name: "AES-GCM",
      length: 256,
    },
    false,
    ["encrypt", "decrypt"]
  )
}

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
  salt: string
  rsaPublicKey: string
  encryptedPrivateKey: string
}

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
    salt: b64(salt),
    rsaPublicKey: b64(spki),
    encryptedPrivateKey: await encryptPrivateKey(privateKey, wrappingKey),
  }
}
