import { useState } from "react"
import { baseip } from "../encryption/ip"
import { deriveKeyFromPassword } from "../encryption/crypto"
import { decryptPrivateKey } from "../encryption/messageCrypto"
import { storePrivateKey } from "../encryption/keyStore"
import { wrapAndStorePrivateKey } from "../encryption/deviceKeyStore"

type Status = "idle" | "loading" | "success" | "error"

interface LoginInput {
  email: string
  password: string
  rememberMe?: boolean
}

interface AuthResponse {
  accessToken: string
  refreshToken: string
  id: number
  email: string
  name: string
  encryptedPrivateKey: string
}

/** Fetch account-specific front salt used for client key derivation. */
async function fetchSalt(email: string): Promise<string> {
  const res = await fetch(
    `${baseip}/auth/salt?email=${encodeURIComponent(email)}`
  )
  if (!res.ok) throw new Error("Could not retrieve account salt.")
  const data = await res.json()
  return data.frontSalt as string
}

/** Hook to perform login, derive keys and optionally wrap the private key to device. */
export function useLogin() {
  const [status, setStatus] = useState<Status>("idle")
  const [error, setError] = useState<string | null>(null)
  const [authData, setAuthData] = useState<AuthResponse | null>(null)

  async function login({
    email,
    password,
    rememberMe = false,
  }: LoginInput): Promise<void> {
    setStatus("loading")
    setError(null)

    try {
      const deviceId = `device_${Date.now()}_${Math.random().toString(36).substring(2, 15)}`

      const frontSaltB64 = await fetchSalt(email)
      const saltBytes = Uint8Array.from(atob(frontSaltB64), (c) =>
        c.charCodeAt(0)
      )

      const loginRes = await fetch(`${baseip}/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password, deviceId }),
      })

      if (!loginRes.ok) {
        let message = "Login failed"
        try {
          const data: unknown = await loginRes.json()
          if (typeof data === "object" && data !== null) {
            const d = data as Record<string, unknown>
            if (typeof d.detail === "string") message = d.detail
            else if (typeof d.title === "string") message = d.title
          }
        } catch {}
        throw new Error(message)
      }

      const data: AuthResponse = await loginRes.json()

      const wrappingKey = await deriveKeyFromPassword(password, saltBytes)
      const privateKey = await decryptPrivateKey(
        data.encryptedPrivateKey,
        wrappingKey
      )
      storePrivateKey(privateKey)

      localStorage.setItem("accessToken", data.accessToken)
      localStorage.setItem("refreshToken", data.refreshToken)
      localStorage.setItem(
        "user",
        JSON.stringify({ id: data.id, email: data.email, name: data.name })
      )
      localStorage.setItem("encryptedPrivateKey", data.encryptedPrivateKey)
      localStorage.setItem("deviceId", deviceId)

      if (rememberMe) {
        await wrapAndStorePrivateKey(privateKey)
        localStorage.setItem("rememberMe", "true")
      } else {
        localStorage.removeItem("rememberMe")
      }

      setAuthData(data)
      setStatus("success")
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Unexpected error occurred")
      setStatus("error")
    }
  }

  return { login, status, error, authData }
}
