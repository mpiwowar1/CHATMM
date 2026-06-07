import { useEffect, useState } from "react"
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

/** Resolve or retrieve cached AES key for a conversation. */
async function resolveAesKey(
  conversationId: number,
  encryptedAesKey: string
): Promise<CryptoKey | null> {
  const cached = getCachedConversationKey(conversationId)
  if (cached) return cached

  const privateKey = getPrivateKey()
  if (!privateKey) return null

  try {
    const key = await decryptConversationKey(encryptedAesKey, privateKey)
    cacheConversationKey(conversationId, key)
    return key
  } catch {
    return null
  }
}

/** Decrypt the stored preview text for a conversation (if present). */
async function decryptPreview(
  conv: ConversationSummaryResponse
): Promise<string | null> {
  if (!conv.lastMessagePreview || !conv.lastMessageIv) return null

  const key = await resolveAesKey(conv.id, conv.encryptedAesKey)
  if (!key) return null

  try {
    return await decryptMessage(
      conv.lastMessagePreview,
      conv.lastMessageIv,
      key
    )
  } catch {
    return null
  }
}

/** Hook that provides decrypted previews for a list of conversations. */
export function useDecryptedPreviews(
  conversations: ConversationSummaryResponse[]
): Record<number, string> {
  const [previews, setPreviews] = useState<Record<number, string>>({})

  useEffect(() => {
    if (conversations.length === 0) return

    let cancelled = false

    const run = async () => {
      const entries = await Promise.all(
        conversations.map(async (conv) => {
          const text = await decryptPreview(conv)
          return [conv.id, text] as [number, string | null]
        })
      )

      if (cancelled) return

      const map: Record<number, string> = {}
      for (const [id, text] of entries) {
        if (text != null) map[id] = text
      }
      setPreviews(map)
    }

    run()
    return () => {
      cancelled = true
    }
  }, [conversations])

  return previews
}
