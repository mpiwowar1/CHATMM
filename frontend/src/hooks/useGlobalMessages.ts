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
import { toWsUrl, getToken } from "@/encryption/utils"
import type { ConversationSummaryResponse } from "@/components/chat-types"

// NOTE: `toWsUrl` and `getToken` are provided by `encryption/utils`

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
  onPreviewUpdate: (update: PreviewUpdate) => void,
  onNewConversation: (conversation: ConversationSummaryResponse) => void
) {
  const stompRef = useRef<Client | null>(null)
  const conversationsRef = useRef(conversations)
  const onPreviewUpdateRef = useRef(onPreviewUpdate)
  const onNewConversationRef = useRef(onNewConversation)
  const subscribedIdsRef = useRef<Set<number>>(new Set())

  useEffect(() => {
    conversationsRef.current = conversations
  }, [conversations])
  useEffect(() => {
    onPreviewUpdateRef.current = onPreviewUpdate
  }, [onPreviewUpdate])
  useEffect(() => {
    onNewConversationRef.current = onNewConversation
  }, [onNewConversation])

  const subscribeToConversation = (
    client: Client,
    conv: ConversationSummaryResponse
  ) => {
    if (subscribedIdsRef.current.has(conv.id)) return
    subscribedIdsRef.current.add(conv.id)

    console.info("Subscribing to conversation topic", conv.id)

    client.subscribe(
      `/topic/conversation.${conv.id}`,
      async (frame: IMessage) => {
        const raw: RawMessage = JSON.parse(frame.body)
        console.info("Received message event", {
          conversationId: raw.conversationId,
          senderName: raw.senderName,
          conversationTopic: conv.id,
        })

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
  }

  /**
   * Hook-level STOMP: subscribe to per-conversation topics and handle preview updates.
   */

  useEffect(() => {
    const token = getToken()
    if (!token) {
      console.warn("Global STOMP not started: missing access token")
      return
    }

    const client = new Client({
      brokerURL: toWsUrl(baseip),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        console.info("Global STOMP connected")

        conversationsRef.current.forEach((conv) =>
          subscribeToConversation(client, conv)
        )

        client.subscribe(`/user/queue/conversations`, (frame: IMessage) => {
          console.info("New conversation notification arrived", frame.body)
          const newConv: ConversationSummaryResponse = JSON.parse(frame.body)
          console.info("Received new conversation notification", newConv)

          subscribeToConversation(client, newConv)

          onNewConversationRef.current(newConv)
        })
      },
      onStompError: (frame) => console.error("Global STOMP error", frame),
      onWebSocketClose: () => console.info("Global STOMP disconnected"),
    })

    client.activate()
    stompRef.current = client

    return () => {
      client.deactivate()
      stompRef.current = null
      subscribedIdsRef.current.clear()
    }
  }, [])

  useEffect(() => {
    const client = stompRef.current
    if (!client?.connected) return
    conversations.forEach((conv) => subscribeToConversation(client, conv))
  }, [conversations])
}
