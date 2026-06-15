export const b64 = (buf: ArrayBuffer | Uint8Array): string => {
  const bytes = buf instanceof Uint8Array ? buf : new Uint8Array(buf)
  return btoa(String.fromCharCode(...bytes))
}

export const fromB64 = (s: string): Uint8Array =>
  Uint8Array.from(atob(s), (c) => c.charCodeAt(0))

/** Convert an HTTP base URL to the WebSocket URL used by STOMP */
export function toWsUrl(base: string): string {
  const url = new URL(base)
  const basePath = url.pathname.replace(/\/$/, "")
  return `${url.protocol.replace("http", "ws")}//${url.host}${basePath}/ws`
}

/** Read access token from localStorage */
export function getToken(): string | null {
  return localStorage.getItem("accessToken")
}

/** Build Authorization header if token is present */
export function getAuthHeader(): Record<string, string> {
  const token = getToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

import { baseip } from "./ip"

function sleep(ms: number) {
  return new Promise((res) => setTimeout(res, ms))
}

/**
 * Attempt to refresh the access token using the stored refresh token.
 * Retries with exponential backoff and jitter.
 */
export async function refreshToken(
  attempts = 3,
  baseDelay = 500,
  reloadOnSuccess = false,
  dispatchEventOnSuccess = false
): Promise<boolean> {
  const refresh = localStorage.getItem("refreshToken")
  if (!refresh) return false

  for (let i = 1; i <= attempts; i++) {
    try {
      const res = await fetch(`${baseip}/auth/refresh`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken: refresh }),
      })

      if (!res.ok) {
        // treat as failure and retry (or break if last attempt)
        if (i === attempts) {
          localStorage.removeItem("accessToken")
          localStorage.removeItem("refreshToken")
          return false
        }
      } else {
        const data = await res.json()
        if (typeof data.accessToken === "string") {
          localStorage.setItem("accessToken", data.accessToken)
        }
        if (typeof data.refreshToken === "string") {
          localStorage.setItem("refreshToken", data.refreshToken)
        }
        if (dispatchEventOnSuccess && typeof window !== "undefined") {
          try {
            window.dispatchEvent(
              new CustomEvent("auth:refreshed", {
                detail: {
                  accessToken: data.accessToken,
                  refreshToken: data.refreshToken,
                },
              })
            )
          } catch {}
        }
        if (reloadOnSuccess && typeof window !== "undefined") {
          window.location.reload()
        }

        return true
      }
    } catch (_) {}

    if (i < attempts) {
      const backoff = baseDelay * Math.pow(2, i - 1)
      const jitter = Math.random() * baseDelay
      await sleep(backoff + jitter)
    }
  }

  localStorage.removeItem("accessToken")
  localStorage.removeItem("refreshToken")
  return false
}
