import { useEffect, useRef, useState, useCallback } from "react"
import { Client, type IMessage } from "@stomp/stompjs"
import { baseip } from "@/encryption/ip"
import {
  decryptConversationKey,
  encryptMessage,
  decryptMessage,
} from "@/encryption/messageCrypto"
import {
  getPrivateKey,
  cacheConversationKey,
  getCachedConversationKey,
} from "@/encryption/keyStore"
import {
  toWsUrl,
  getToken,
  getAuthHeader,
  refreshToken,
} from "@/encryption/utils"

export type DecryptedMessage = {
  id: number
  conversationId: number
  senderId: number
  senderName: string
  text: string
  timestamp: string
  failed?: boolean
}

type RawMessage = {
  id: number
  conversationId: number
  senderId: number
  senderName: string
  ciphertext: string
  iv: string
  timestamp: string
}

/**
 * Get cached AES key for a conversation or decrypt it with the session RSA key.
 */
async function getOrDecryptConversationKey(
  conversationId: number,
  encryptedAesKey: string
): Promise<CryptoKey | null> {
  const cached = getCachedConversationKey(conversationId)
  if (cached) return cached

  const privateKey = getPrivateKey()
  if (!privateKey) {
    console.error("No private key in memory — user must re-login.")
    return null
  }

  try {
    const aesKey = await decryptConversationKey(encryptedAesKey, privateKey)
    cacheConversationKey(conversationId, aesKey)
    return aesKey
  } catch {
    console.error("Failed to decrypt conversation key")
    return null
  }
}

/**
 * Decrypt a RawMessage into a DecryptedMessage using the conversation AES key.
 */
async function decryptRaw(
  msg: RawMessage,
  aesKey: CryptoKey
): Promise<DecryptedMessage> {
  try {
    const text = await decryptMessage(msg.ciphertext, msg.iv, aesKey)
    return { ...msg, text }
  } catch {
    return { ...msg, text: "[decryption failed]", failed: true }
  }
}

/**
 * Fetch paginated raw message history for a conversation from the API.
 */
async function fetchHistory(
  conversationId: number,
  cursor: number | null,
  limit: number
): Promise<{
  messages: RawMessage[]
  nextCursor: number | null
  hasMore: boolean
}> {
  const params = new URLSearchParams({ limit: String(limit) })
  if (cursor != null) params.set("cursor", String(cursor))

  const res = await fetch(
    `${baseip}/conversations/${conversationId}/messages?${params}`,
    { headers: { "Content-Type": "application/json", ...getAuthHeader() } }
  )

  if (!res.ok) throw new Error("Failed to load messages")
  return res.json()
}

/**
 * Hook managing messages, history loading and STOMP for a conversation.
 */
export function useChat(
  conversationId: number | null,
  encryptedAesKey: string | null
) {
  const [messages, setMessages] = useState<DecryptedMessage[]>([])
  const [loading, setLoading] = useState(false)
  const [loadingMore, setLoadingMore] = useState(false)
  const [hasMore, setHasMore] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [connected, setConnected] = useState(false)

  const cursorRef = useRef<number | null>(null)
  const aesKeyRef = useRef<CryptoKey | null>(null)
  const encryptedAesKeyRef = useRef<string | null>(null) // ← NEW
  const stompRef = useRef<Client | null>(null)
  const LIMIT = 50

  // Keep encryptedAesKeyRef always current
  useEffect(() => {
    encryptedAesKeyRef.current = encryptedAesKey
  }, [encryptedAesKey])

  // Resolve and cache the AES key whenever the conversation or key changes
  useEffect(() => {
    if (!conversationId || !encryptedAesKey) return
    aesKeyRef.current = null

    getOrDecryptConversationKey(conversationId, encryptedAesKey).then((key) => {
      aesKeyRef.current = key
    })
  }, [conversationId, encryptedAesKey])

  const loadHistory = useCallback(async () => {
    if (!conversationId || !encryptedAesKeyRef.current) return
    setLoading(true)
    setError(null)
    cursorRef.current = null

    try {
      const aesKey = await getOrDecryptConversationKey(
        conversationId,
        encryptedAesKeyRef.current
      )
      if (!aesKey) throw new Error("Could not decrypt conversation key.")
      aesKeyRef.current = aesKey

      const {
        messages: raw,
        nextCursor,
        hasMore: more,
      } = await fetchHistory(conversationId, null, LIMIT)
      const decrypted = await Promise.all(raw.map((m) => decryptRaw(m, aesKey)))
      setMessages(decrypted)
      setHasMore(more)
      cursorRef.current = nextCursor
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load messages")
    } finally {
      setLoading(false)
    }
  }, [conversationId]) // ← encryptedAesKey removed, using ref instead

  const loadMore = useCallback(async () => {
    if (!conversationId || !aesKeyRef.current || !hasMore || loadingMore) return
    setLoadingMore(true)

    try {
      const {
        messages: raw,
        nextCursor,
        hasMore: more,
      } = await fetchHistory(conversationId, cursorRef.current, LIMIT)
      const decrypted = await Promise.all(
        raw.map((m) => decryptRaw(m, aesKeyRef.current!))
      )
      setMessages((prev) => [...decrypted, ...prev])
      setHasMore(more)
      cursorRef.current = nextCursor
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to load more messages"
      )
    } finally {
      setLoadingMore(false)
    }
  }, [conversationId, hasMore, loadingMore])

  // STOMP — only reconnects when conversationId changes
  useEffect(() => {
    if (!conversationId) return
    const token = getToken()
    if (!token) return

    const client = new Client({
      brokerURL: toWsUrl(baseip),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        setConnected(true)

        client.subscribe(
          `/topic/conversation.${conversationId}`,
          async (frame: IMessage) => {
            const raw: RawMessage = JSON.parse(frame.body)

            let aesKey = aesKeyRef.current
            if (!aesKey && encryptedAesKeyRef.current) {
              aesKey = await getOrDecryptConversationKey(
                conversationId,
                encryptedAesKeyRef.current
              )
              aesKeyRef.current = aesKey
            }
            if (!aesKey) return

            const msg = await decryptRaw(raw, aesKey)
            setMessages((prev) => {
              if (prev.some((m) => m.id === msg.id)) return prev
              return [...prev, msg]
            })
          }
        )
      },
      onDisconnect: () => setConnected(false),
      onStompError: async (frame) => {
        console.error("STOMP error", frame)
        setError("WebSocket connection error")

        const ok = await refreshToken(4, 500)
        if (!ok) return

        const newToken = getToken()
        if (!newToken) return

        try {
          client.deactivate()
        } catch {}

        client.connectHeaders = { Authorization: `Bearer ${newToken}` }
        client.activate()
      },
    })

    client.activate()
    stompRef.current = client

    return () => {
      client.deactivate()
      stompRef.current = null
      setConnected(false)
    }
  }, [conversationId])

  useEffect(() => {
    setMessages([])
    setHasMore(false)
    cursorRef.current = null
    loadHistory()
  }, [loadHistory])

  const sendMessage = useCallback(
    async (text: string): Promise<void> => {
      if (!conversationId || !stompRef.current?.connected) {
        setError("Not connected")
        return
      }

      const aesKey = aesKeyRef.current
      if (!aesKey) {
        setError("Conversation key not available")
        return
      }

      try {
        const { ciphertext, iv } = await encryptMessage(text, aesKey)
        stompRef.current.publish({
          destination: "/app/chat.send",
          body: JSON.stringify({ conversationId, ciphertext, iv }),
        })
      } catch {
        setError("Failed to encrypt or send message")
      }
    },
    [conversationId]
  )

  return {
    messages,
    loading,
    loadingMore,
    hasMore,
    error,
    connected,
    sendMessage,
    loadMore,
    reload: loadHistory,
  }
}
