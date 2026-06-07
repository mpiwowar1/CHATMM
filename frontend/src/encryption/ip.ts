// Prefer the Vite-provided env var. If not present (e.g. build missed the arg),
// use the browser's current origin so the app talks to the same host it's served from.
// Finally fall back to localhost for local dev convenience.
const env = import.meta.env as Record<string, unknown>
const rawBase =
  (env.VITE_API_BASE as string | undefined) ||
  (env.VITE_API_URL as string | undefined)

const originFallback =
  typeof window !== "undefined" && window.location && window.location.origin
    ? `${window.location.origin}/api/v1`
    : undefined

const finalBase =
  rawBase && rawBase.length > 0
    ? rawBase
    : (originFallback ?? "http://localhost:8086/api/v1")

export const baseip = finalBase.replace(/\/+$/g, "")
