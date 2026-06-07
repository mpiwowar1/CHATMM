import { useEffect, useRef, useState } from "react"
import Sidebar from "./Sidebar"
import { ChatArea } from "./ChatArea"
import type { ConversationSummaryResponse, UserResponse } from "./chat-types"
import useConversations from "@/hooks/useConversations"
import { useChat } from "@/hooks/useChat"
import { useDecryptedPreviews } from "@/hooks/useDecryptedPreviews"
import { useGlobalMessages } from "@/hooks/useGlobalMessages"

function getCurrentUser(): UserResponse {
  const stored =
    typeof window !== "undefined" ? localStorage.getItem("user") : null
  const parsed: Partial<UserResponse> | null = stored
    ? JSON.parse(stored)
    : null
  return {
    id: parsed?.id ?? 0,
    email: parsed?.email ?? "",
    name: parsed?.name ?? "Guest",
    publicKey: parsed?.publicKey ?? "",
  }
}

export function ChatLayout({ onLogout }: { onLogout?: () => void }) {
  const currentUser = getCurrentUser()

  const {
    conversations,
    loading,
    error,
    fetchConversations,
    createConversation,
    updateConversationPreview,
    addConversation,
  } = useConversations()

  const [activeId, setActiveId] = useState<number | null>(null)
  const [previewOverrides, setPreviewOverrides] = useState<
    Record<number, string>
  >({})
  const refreshTimeoutRef = useRef<number | null>(null)

  const handleNewConversation = (conversation: ConversationSummaryResponse) => {
    addConversation(conversation)

    if (refreshTimeoutRef.current) {
      window.clearTimeout(refreshTimeoutRef.current)
    }

    refreshTimeoutRef.current = window.setTimeout(() => {
      fetchConversations()
    }, 800)
  }

  useEffect(() => {
    return () => {
      if (refreshTimeoutRef.current) {
        window.clearTimeout(refreshTimeoutRef.current)
      }
    }
  }, [])

  useGlobalMessages(
    conversations,
    activeId,
    ({ conversationId, text, senderName, timestamp }) => {
      setPreviewOverrides((prev) => ({ ...prev, [conversationId]: text }))
      updateConversationPreview(conversationId, senderName, timestamp)
    },
    handleNewConversation
  )

  useEffect(() => {
    if (conversations.length > 0 && activeId === null) {
      setActiveId(conversations[0].id)
    }
  }, [conversations, activeId])

  const activeConversation: ConversationSummaryResponse | undefined =
    conversations.find((c) => c.id === activeId)

  const {
    messages,
    loading: chatLoading,
    loadingMore,
    hasMore,
    error: chatError,
    connected,
    sendMessage,
    loadMore,
  } = useChat(activeId, activeConversation?.encryptedAesKey ?? null)

  const lastMessage = messages[messages.length - 1]

  useEffect(() => {
    if (!lastMessage || !activeId) return
    setPreviewOverrides((prev) => ({ ...prev, [activeId]: lastMessage.text }))
    updateConversationPreview(
      activeId,
      lastMessage.senderName,
      lastMessage.timestamp
    )
  }, [lastMessage?.id])

  const decryptedPreviews = useDecryptedPreviews(conversations)

  const mergedPreviews: Record<number, string> = {
    ...decryptedPreviews,
    ...previewOverrides,
  }

  return (
    <div className="flex h-screen overflow-hidden bg-background text-foreground">
      <Sidebar
        currentUser={currentUser}
        conversations={conversations}
        activeId={activeId}
        onSelect={setActiveId}
        onCreateConversation={createConversation}
        decryptedPreviews={mergedPreviews}
        onLogout={onLogout}
      />
      {activeConversation ? (
        <ChatArea
          currentUserId={currentUser.id}
          conversation={activeConversation}
          messages={messages}
          loading={chatLoading}
          loadingMore={loadingMore}
          hasMore={hasMore}
          connected={connected}
          error={chatError}
          onSend={sendMessage}
          onLoadMore={loadMore}
        />
      ) : (
        <div className="flex flex-1 items-center justify-center text-muted-foreground">
          {loading
            ? "Loading…"
            : (error ?? "Select a conversation to start chatting")}
        </div>
      )}
    </div>
  )
}

export default ChatLayout
