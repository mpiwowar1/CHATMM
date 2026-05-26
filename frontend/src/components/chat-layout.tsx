import { useEffect, useState } from "react"
import Sidebar from "./Sidebar"
import { ChatArea } from "./ChatArea"
import type {
  ChatMessageResponse,
  ConversationSummaryResponse,
  UserResponse,
} from "./chat-types"
import useConversations from "@/hooks/useConversations"

export function ChatLayout() {
  // Try to read current user from localStorage (set on login)
  const stored = typeof window !== "undefined" ? localStorage.getItem("user") : null
  const parsed: Partial<UserResponse> | null = stored ? JSON.parse(stored) : null

  const currentUser: UserResponse = {
    id: parsed?.id ?? 1,
    email: parsed?.email ?? "guest@example.com",
    name: parsed?.name ?? "guest",
    publicKey: parsed?.publicKey ?? "",
  }

  const { conversations, loading, error, fetchConversations, createConversation } =
    useConversations()

  const [activeId, setActiveId] = useState<number | null>(conversations[0]?.id ?? null)
  const [messagesByConv, setMessagesByConv] = useState<
    Record<number, ChatMessageResponse[]>
  >({})
  const [decryptedByConv, setDecryptedByConv] = useState<
    Record<number, Record<number, string>>
  >({})

  useEffect(() => {
    if (conversations.length > 0 && activeId === null) {
      setActiveId(conversations[0].id)
    }
    // keep activeId if conversations change
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [conversations])

  useEffect(() => {
    // refresh conversations on mount
    fetchConversations()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const activeConversation = conversations.find((c) => c.id === activeId)!
  const activeMessages = (activeId && messagesByConv[activeId]) ?? []
  const activeDecrypted = (activeId && decryptedByConv[activeId]) ?? {}

  const handleSend = (plaintext: string) => {
    if (!activeId) return
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

  const handleNewConversation = async () => {
    const name = window.prompt("Conversation name (optional)")
    const participantInput = window.prompt("Participant IDs (comma separated)")
    if (!participantInput) return
    const ids = participantInput
      .split(",")
      .map((s) => parseInt(s.trim(), 10))
      .filter((n) => !Number.isNaN(n))

    if (ids.length === 0) {
      alert("Please provide at least one participant id")
      return
    }

    const newId = await createConversation(name ?? null, ids)
    if (newId) {
      setActiveId(newId)
    }
  }

  return (
    <div className="flex h-screen overflow-hidden bg-background text-foreground">
      <Sidebar
        currentUser={currentUser}
        conversations={conversations}
        activeId={activeId ?? null}
        onSelect={(id) => setActiveId(id)}
        onCreateConversation={createConversation}
      />
      {activeConversation ? (
        <ChatArea
          currentUserId={currentUser.id}
          conversation={activeConversation}
          messages={activeMessages}
          decryptedMap={activeDecrypted}
          onSend={handleSend}
        />
      ) : (
        <div className="flex-1 flex items-center justify-center">{loading ? "Loading..." : error ?? "No conversation selected"}</div>
      )}
    </div>
  )
}

export default ChatLayout
