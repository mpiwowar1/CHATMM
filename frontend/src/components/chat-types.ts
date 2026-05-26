export type ConversationType = "DIRECT" | "GROUP"
export type ParticipantRole = "ADMIN" | "MEMBER"

export interface UserResponse {
  id: number
  email: string
  name: string
  publicKey: string
}

export interface ConversationParticipantResponse {
  id: number
  email: string
  name: string
  role: ParticipantRole
}

export interface ConversationSummaryResponse {
  id: number
  name: string
  type: ConversationType
  encryptedAesKey: string
  lastMessageAt: string | null
  lastMessagePreview: string | null   // encrypted ciphertext
  lastMessageIv: string | null
  lastMessageSenderName: string | null
  participants: ConversationParticipantResponse[]
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

export interface NotificationResponse {
  conversationId: number
  senderId: number
  senderName: string
  timestamp: string
}

export interface ErrorResponse {
  type: string
  title: string
  status: number
  detail: string
  instance: string
}