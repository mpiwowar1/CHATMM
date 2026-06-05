import { useEffect, useRef } from "react"
import { Client, type IMessage } from "@stomp/stompjs"
import { baseip } from "@/encryption/ip"
import {
  getPrivateKey,
  getCachedConversationKey,
  cacheConversationKey,
} from "@/encryption/keyStore"
import {
  decryptConversationKey,
  decryptMessage,
} from "@/encryption/messageCrypto"
import type { ConversationSummaryResponse } from "@/components/chat-types"

function toWsUrl(base: string): string {
  const url = new URL(base)
  const basePath = url.pathname.replace(/\/$/, "")
  return `${url.protocol.replace("http", "ws")}//${url.host}${basePath}/ws`
}

function getToken(): string | null {
  return localStorage.getItem("accessToken")
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

type PreviewUpdate = {
  conversationId: number
  text: string
  senderName: string
  timestamp: string
}

export function useGlobalMessages(
  conversations: ConversationSummaryResponse[],
  activeConversationId: number | null,
  onPreviewUpdate: (update: PreviewUpdate) => void
) {
  const stompRef = useRef<Client | null>(null)
  const conversationsRef = useRef(conversations)
  const onPreviewUpdateRef = useRef(onPreviewUpdate)

  // Always keep refs current without reconnecting STOMP
  useEffect(() => {
    conversationsRef.current = conversations
  }, [conversations])
  useEffect(() => {
    onPreviewUpdateRef.current = onPreviewUpdate
  }, [onPreviewUpdate])

  useEffect(() => {
    const token = getToken()
    if (!token) return

    const client = new Client({
      brokerURL: toWsUrl(baseip),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        // Subscribe to every conversation the user is part of
        conversationsRef.current.forEach((conv) => {
          client.subscribe(
            `/topic/conversation.${conv.id}`,
            async (frame: IMessage) => {
              const raw: RawMessage = JSON.parse(frame.body)

              // Skip active conversation — useChat handles that one
              if (raw.conversationId === activeConversationId) return

              // Resolve AES key
              let aesKey = getCachedConversationKey(conv.id)
              if (!aesKey) {
                const privateKey = getPrivateKey()
                if (!privateKey) return
                try {
                  aesKey = await decryptConversationKey(
                    conv.encryptedAesKey,
                    privateKey
                  )
                  cacheConversationKey(conv.id, aesKey)
                } catch {
                  return
                }
              }

              // Decrypt and emit preview update
              try {
                const text = await decryptMessage(
                  raw.ciphertext,
                  raw.iv,
                  aesKey
                )
                onPreviewUpdateRef.current({
                  conversationId: conv.id,
                  text,
                  senderName: raw.senderName,
                  timestamp: raw.timestamp,
                })
              } catch {
                // ignore decryption failures on preview
              }
            }
          )
        })
      },
      onStompError: (frame) => console.error("Global STOMP error", frame),
    })

    client.activate()
    stompRef.current = client

    return () => {
      client.deactivate()
      stompRef.current = null
    }
  }, []) // Only connect once — conversations added via ref

  // When conversations list changes (new conversation added), subscribe to new ones
  useEffect(() => {
    const client = stompRef.current
    if (!client?.connected) return

    conversations.forEach((conv) => {
      // @stomp/stompjs deduplicates subscriptions by destination internally
      client.subscribe(
        `/topic/conversation.${conv.id}`,
        async (frame: IMessage) => {
          const raw: RawMessage = JSON.parse(frame.body)
          if (raw.conversationId === activeConversationId) return

          let aesKey = getCachedConversationKey(conv.id)
          if (!aesKey) {
            const privateKey = getPrivateKey()
            if (!privateKey) return
            try {
              aesKey = await decryptConversationKey(
                conv.encryptedAesKey,
                privateKey
              )
              cacheConversationKey(conv.id, aesKey)
            } catch {
              return
            }
          }

          try {
            const text = await decryptMessage(raw.ciphertext, raw.iv, aesKey)
            onPreviewUpdateRef.current({
              conversationId: conv.id,
              text,
              senderName: raw.senderName,
              timestamp: raw.timestamp,
            })
          } catch {}
        }
      )
    })
  }, [conversations])
}
