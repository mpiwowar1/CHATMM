import { useEffect, useState } from "react"
import { baseip } from "@/encryption/ip"
import {
  buildParticipantKeysMap,
  type ParticipantKey,
} from "@/encryption/conversationCrypto"
import type { ConversationSummaryResponse } from "@/components/chat-types"

function getAuthHeader(): Record<string, string> {
  const token = localStorage.getItem("accessToken")
  return token ? { Authorization: `Bearer ${token}` } : {}
}

async function fetchMyPublicKey(): Promise<ParticipantKey> {
  const res = await fetch(`${baseip}/users/me`, {
    headers: { "Content-Type": "application/json", ...getAuthHeader() },
  })
  if (!res.ok) throw new Error("Failed to fetch own user info")
  const data = await res.json()
  return { id: data.id, publicKey: data.publicKey }
}

export function useConversations(page = 0, size = 50) {
  const [conversations, setConversations] = useState<
    ConversationSummaryResponse[]
  >([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function addConversation(conv: ConversationSummaryResponse) {
    setConversations((prev) =>
      prev.some((c) => c.id === conv.id) ? prev : [conv, ...prev]
    )
  }

  function updateConversationPreview(
    conversationId: number,
    senderName: string,
    lastMessageAt: string
  ) {
    setConversations((prev) =>
      prev.map((c) =>
        c.id === conversationId
          ? { ...c, lastMessageSenderName: senderName, lastMessageAt }
          : c
      )
    )
  }

  async function fetchConversations() {
    setLoading(true)
    setError(null)
    try {
      const res = await fetch(
        `${baseip}/conversations?page=${page}&size=${size}`,
        {
          headers: { "Content-Type": "application/json", ...getAuthHeader() },
        }
      )

      if (!res.ok) {
        if (res.status === 401 || res.status === 403) {
          setError("Unauthorized")
          return
        }
        throw new Error("Failed to load conversations")
      }

      const data = await res.json()
      setConversations(Array.isArray(data.content) ? data.content : [])
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Unexpected error")
    } finally {
      setLoading(false)
    }
  }

  async function createConversation(
    name: string | null,
    participants: ParticipantKey[]
  ): Promise<number | null> {
    setLoading(true)
    setError(null)

    try {
      const me = await fetchMyPublicKey()

      const allParticipants: ParticipantKey[] = [
        me,
        ...participants.filter((p) => p.id !== me.id),
      ]

      const participantKeys = await buildParticipantKeysMap(allParticipants)

      const res = await fetch(`${baseip}/conversations`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...getAuthHeader() },
        body: JSON.stringify({ name, participantKeys }),
      })

      if (!res.ok) {
        if (res.status === 401 || res.status === 403) {
          setError("Unauthorized")
          return null
        }
        let message = "Failed to create conversation"
        try {
          const errData = await res.json()
          if (errData?.detail) message = errData.detail
        } catch {}
        throw new Error(message)
      }

      const data = await res.json()
      await fetchConversations()
      return data?.conversationId ?? null
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Unexpected error")
      return null
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchConversations()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, size])

  return {
    conversations,
    loading,
    error,
    fetchConversations,
    createConversation,
    updateConversationPreview,
    addConversation,
  }
}

export default useConversations
