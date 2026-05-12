import { useState } from "react"
import { buildRegistrationPayload } from "../encryption/crypto"
import { baseip } from "../encryption/ip"

type Status = "idle" | "loading" | "success" | "error"

interface RegisterInput {
  name: string
  email: string
  password: string
}

export function useRegister() {
  const [status, setStatus] = useState<Status>("idle")
  const [error, setError] = useState<string | null>(null)

  async function register({
    name,
    email,
    password,
  }: RegisterInput): Promise<void> {
    setStatus("loading")
    setError(null)

    try {
      const payload = await buildRegistrationPayload(name, email, password)

      const res = await fetch(baseip + "/auth/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      })

      if (!res.ok) {
        let message = "Registration failed"

        try {
          const data: unknown = await res.json()
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

      setStatus("success")
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : "Unexpected error occurred"

      setError(message)
      setStatus("error")
    }
  }

  return { register, status, error }
}
