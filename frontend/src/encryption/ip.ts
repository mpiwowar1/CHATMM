/**
 * API base URL.
 * Prefer configuring via Vite env `VITE_API_BASE` (e.g. https://api.example.com/api/v1).
 * Falls back to localhost when not provided.
 */
const DEFAULT_BASE = "http://localhost:8080/api/v1"

const env = import.meta.env as Record<string, unknown>
const rawBase =
  (env.VITE_API_BASE as string | undefined) ||
  (env.VITE_API_URL as string | undefined)

export const baseip = (
  rawBase && rawBase.length > 0 ? rawBase : DEFAULT_BASE
).replace(/\/+$/g, "")
