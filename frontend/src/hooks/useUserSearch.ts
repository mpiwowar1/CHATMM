import { useEffect, useState } from "react"
import { baseip } from "@/encryption/ip"
import { getAuthHeader } from "@/encryption/utils"

export type AutocompleteUser = {
  name: string
  email: string
}

/** Hook to search users for autocomplete (debounced). */
export function useUserSearch(query: string, limit = 6, enabled = true) {
  const [results, setResults] = useState<AutocompleteUser[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!enabled) {
      setResults([])
      return
    }

    if (!query || query.trim().length === 0) {
      setResults([])
      return
    }

    let mounted = true
    const controller = new AbortController()

    const fetchResults = async () => {
      setLoading(true)
      setError(null)
      try {
        const res = await fetch(
          `${baseip}/users/autocomplete?query=${encodeURIComponent(query)}&limit=${limit}`,
          {
            headers: getAuthHeader(),
            signal: controller.signal,
          }
        )

        if (!res.ok) {
          if (res.status === 401 || res.status === 403) {
            if (mounted) setError("Unauthorized")
            return
          }
          throw new Error("Failed to search users")
        }

        const data = await res.json()
        if (mounted) setResults(data ?? [])
      } catch (err: unknown) {
        if ((err as { name?: string })?.name === "AbortError") return
        setError(err instanceof Error ? err.message : "Unexpected error")
      } finally {
        if (mounted) setLoading(false)
      }
    }

    const t = setTimeout(fetchResults, 200)

    return () => {
      mounted = false
      controller.abort()
      clearTimeout(t)
    }
  }, [query, limit, enabled])

  return { results, loading, error }
}

export default useUserSearch
