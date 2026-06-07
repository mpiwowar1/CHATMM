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
