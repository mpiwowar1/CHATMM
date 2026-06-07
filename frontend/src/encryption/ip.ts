const DEFAULT_BASE = "http://localhost:8080/api/v1"

const env = import.meta.env as Record<string, unknown>
const rawBase =
  (env.VITE_API_BASE as string | undefined) ||
  (env.VITE_API_URL as string | undefined)

export const baseip = (
  rawBase && rawBase.length > 0 ? rawBase : DEFAULT_BASE
).replace(/\/+$/g, "")
