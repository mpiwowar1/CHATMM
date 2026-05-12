export type ConversationType = "DIRECT" | "GROUP"

export interface UserResponse {
  id: number
  email: string
  name: string
  publicKey: string
}

export interface ConversationSummaryResponse {
  id: number
  name: string
  type: ConversationType
  encryptedAesKey: string
}

export interface ChatMessageResponse {
  id: number
  conversationId: number
  senderId: number
  senderName: string
  ciphertext: string
  iv: string
  timestamp: string
}

export interface MessagePageResponse {
  messages: ChatMessageResponse[]
  nextCursor: number | null
  hasMore: boolean
}

export interface ChatMessagePayload {
  conversationId: number
  ciphertext: string
  iv: string
}
