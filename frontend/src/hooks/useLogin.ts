import { useState } from "react"
import { baseip } from "../encryption/ip"

type Status = "idle" | "loading" | "success" | "error"

interface LoginInput {
  email: string
  password: string
}

interface AuthResponse {
  accessToken: string
  refreshToken: string
  id: number
  email: string
  name: string
  encryptedPrivateKey: string
}

export function useLogin() {
  const [status, setStatus] = useState<Status>("idle")
  const [error, setError] = useState<string | null>(null)
  const [authData, setAuthData] = useState<AuthResponse | null>(null)

  async function login({ email, password }: LoginInput): Promise<void> {
    setStatus("loading")
    setError(null)

    try {
      const deviceId = `device_${Date.now()}_${Math.random().toString(36).substring(2, 15)}`

      const loginRes = await fetch(baseip + "/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          email,
          password,
          deviceId,
        }),
      })

      if (!loginRes.ok) {
        let message = "Login failed"

        try {
          const data: unknown = await loginRes.json()
          if (
            typeof data === "object" &&
            data !== null &&
            "detail" in data &&
            typeof (data as any).detail === "string"
          ) {
            message = (data as any).detail
          } else if (
            typeof data === "object" &&
            data !== null &&
            "title" in data &&
            typeof (data as any).title === "string"
          ) {
            message = (data as any).title
          }
        } catch {
        }

        throw new Error(message)
      }

      const data: AuthResponse = await loginRes.json()

  
      localStorage.setItem("accessToken", data.accessToken)
      localStorage.setItem("refreshToken", data.refreshToken)
      localStorage.setItem(
        "user",
        JSON.stringify({
          id: data.id,
          email: data.email,
          name: data.name,
        })
      )
      // Persist encrypted private key and full auth payload for later use
      if (data.encryptedPrivateKey) {
        localStorage.setItem("encryptedPrivateKey", data.encryptedPrivateKey)
      }

      // Save full auth response for convenience
      try {
        localStorage.setItem("authData", JSON.stringify(data))
      } catch {}

      // Store the device id used for this login so the client can reuse it
      try {
        localStorage.setItem("deviceId", deviceId)
      } catch {}

      setAuthData(data)
      setStatus("success")
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : "Unexpected error occurred"

      setError(message)
      setStatus("error")
    }
  }

  return { login, status, error, authData }
}
