import { useEffect, useState } from "react"
import Sidebar from "./Sidebar"
import { ChatArea } from "./ChatArea"
import type { ConversationSummaryResponse, UserResponse } from "./chat-types"
import useConversations from "@/hooks/useConversations"
import { useChat } from "@/hooks/useChat"

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

export function ChatLayout() {
  const currentUser = getCurrentUser()

  const {
    conversations,
    loading,
    error,
    fetchConversations,
    createConversation,
  } = useConversations()

  const [activeId, setActiveId] = useState<number | null>(null)

  // Set first conversation as active once loaded
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

  return (
    <div className="flex h-screen overflow-hidden bg-background text-foreground">
      <Sidebar
        currentUser={currentUser}
        conversations={conversations}
        activeId={activeId}
        onSelect={setActiveId}
        onCreateConversation={createConversation}
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
