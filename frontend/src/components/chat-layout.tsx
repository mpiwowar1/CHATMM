import { useState } from "react"
import Sidebar from "./Sidebar"
import { ChatArea } from "./ChatArea"
import type {
  ChatMessageResponse,
  ConversationSummaryResponse,
  UserResponse,
} from "./chat-types"

export function ChatLayout() {
  const currentUser: UserResponse = {
    id: 1,
    email: "maks@example.com",
    name: "maks",
    publicKey: "-----BEGIN PUBLIC KEY-----",
  }

  const conversations: ConversationSummaryResponse[] = [
    { id: 101, name: "Kuba", type: "DIRECT", encryptedAesKey: "enc-key-1" },
    { id: 102, name: "Dev Team", type: "GROUP", encryptedAesKey: "enc-key-2" },
    {
      id: 103,
      name: "Jakub",
      type: "DIRECT",
      encryptedAesKey: "enc-key-3",
    },
  ]

  const seedMessages: ChatMessageResponse[] = [
    {
      id: 1,
      conversationId: 101,
      senderId: 2,
      senderName: "Kuba",
      ciphertext: "enc1",
      iv: "iv1",
      timestamp: new Date(Date.now() - 10 * 60000).toISOString(),
    },
    {
      id: 2,
      conversationId: 101,
      senderId: 1,
      senderName: "HSBADF",
      ciphertext: "enc2",
      iv: "iv2",
      timestamp: new Date(Date.now() - 8 * 60000).toISOString(),
    },
    {
      id: 3,
      conversationId: 101,
      senderId: 2,
      senderName: "Jakub",
      ciphertext: "enc3",
      iv: "iv3",
      timestamp: new Date(Date.now() - 3 * 60000).toISOString(),
    },
  ]

  const seedDecrypted: Record<number, string> = {
    1: "1?",
    2: "2",
    3: "3",
  }

  const [activeId, setActiveId] = useState<number>(101)
  const [messagesByConv, setMessagesByConv] = useState<
    Record<number, ChatMessageResponse[]>
  >({
    101: seedMessages,
    102: [],
    103: [],
  })
  const [decryptedByConv, setDecryptedByConv] = useState<
    Record<number, Record<number, string>>
  >({
    101: seedDecrypted,
  })

  const activeConversation = conversations.find((c) => c.id === activeId)!
  const activeMessages = messagesByConv[activeId] ?? []
  const activeDecrypted = decryptedByConv[activeId] ?? {}

  const handleSend = (plaintext: string) => {
    const tempId = Date.now()
    const outgoing: ChatMessageResponse = {
      id: tempId,
      conversationId: activeId,
      senderId: currentUser.id,
      senderName: currentUser.name,
      ciphertext: btoa(plaintext),
      iv: crypto.randomUUID(),
      timestamp: new Date().toISOString(),
    }

    setMessagesByConv((prev) => ({
      ...prev,
      [activeId]: [...(prev[activeId] ?? []), outgoing],
    }))
    setDecryptedByConv((prev) => ({
      ...prev,
      [activeId]: { ...(prev[activeId] ?? {}), [tempId]: plaintext },
    }))
  }

  return (
    <div className="flex h-screen overflow-hidden bg-background text-foreground">
      <Sidebar
        currentUser={currentUser}
        conversations={conversations}
        activeId={activeId}
        onSelect={setActiveId}
      />
      <ChatArea
        currentUserId={currentUser.id}
        conversation={activeConversation}
        messages={activeMessages}
        decryptedMap={activeDecrypted}
        onSend={handleSend}
      />
    </div>
  )
}

export default ChatLayout
