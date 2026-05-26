import { useEffect, useRef, useCallback } from "react"
import { ScrollArea } from "@/components/ui/scroll-area"
import type { ConversationSummaryResponse } from "./chat-types"
import type { DecryptedMessage } from "@/hooks/useChat"
import ChatHeader from "./ChatHeader"
import { ChatInput } from "./ChatInput"
import { MessageBubble } from "./MessageBubble"
import { Loader2 } from "lucide-react"

export function ChatArea({
  currentUserId,
  conversation,
  messages,
  loading,
  loadingMore,
  hasMore,
  connected,
  error,
  onSend,
  onLoadMore,
}: {
  currentUserId: number
  conversation: ConversationSummaryResponse
  messages: DecryptedMessage[]
  loading: boolean
  loadingMore: boolean
  hasMore: boolean
  connected: boolean
  error: string | null
  onSend: (plaintext: string) => Promise<void>
  onLoadMore: () => void
}) {
  const bottomRef = useRef<HTMLDivElement>(null)
  const scrollAreaRef = useRef<HTMLDivElement>(null)
  const prevMessageCountRef = useRef(messages.length)

  // Scroll to bottom only when new messages arrive at the bottom
  // (not when older messages are prepended via loadMore)
  const lastMessageIdRef = useRef<number | undefined>(undefined)

  useEffect(() => {
    const lastMsg = messages[messages.length - 1]
    if (!lastMsg) return

    // Only scroll to bottom if a genuinely new message arrived at the end
    if (lastMsg.id !== lastMessageIdRef.current) {
      const wasAtBottom =
        lastMessageIdRef.current !== undefined &&
        messages.findIndex((m) => m.id === lastMessageIdRef.current) !== 0

      if (wasAtBottom || lastMessageIdRef.current === undefined) {
        bottomRef.current?.scrollIntoView({ behavior: "smooth" })
      }
    }

    lastMessageIdRef.current = lastMsg.id
  }, [messages])

  // Scroll to bottom on initial load
  useEffect(() => {
    if (!loading && messages.length > 0) {
      bottomRef.current?.scrollIntoView({ behavior: "instant" })
    }
  }, [loading])

  // Trigger loadMore when user scrolls to the top
  const handleScroll = useCallback(
    (e: React.UIEvent<HTMLDivElement>) => {
      if (e.currentTarget.scrollTop === 0 && hasMore && !loadingMore) {
        onLoadMore()
      }
    },
    [hasMore, loadingMore, onLoadMore]
  )

  return (
    <div className="flex h-screen min-w-0 flex-1 flex-col">
      <ChatHeader conversation={conversation} connected={connected} />

      <ScrollArea
        className="flex-1 px-5 py-4"
        onScrollCapture={handleScroll}
        ref={scrollAreaRef}
      >
        <div className="flex flex-col gap-3">
          {/* Load more trigger at the top */}
          {hasMore && (
            <div className="flex justify-center py-2">
              {loadingMore ? (
                <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
              ) : (
                <button
                  onClick={onLoadMore}
                  className="text-xs text-muted-foreground hover:text-foreground"
                >
                  Load older messages
                </button>
              )}
            </div>
          )}

          {/* Initial load spinner */}
          {loading && (
            <div className="flex justify-center py-8">
              <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
            </div>
          )}

          {/* Error banner */}
          {error && (
            <div className="rounded bg-destructive/10 px-3 py-2 text-center text-sm text-destructive">
              {error}
            </div>
          )}

          {/* Messages */}
          {!loading &&
            messages.map((msg, idx) => {
              const isOwn = msg.senderId === currentUserId
              const prev = messages[idx - 1]
              const showSenderName =
                !isOwn && (!prev || prev.senderId !== msg.senderId)

              return (
                <MessageBubble
                  key={msg.id}
                  message={msg}
                  isOwn={isOwn}
                  showSenderName={showSenderName}
                />
              )
            })}

          <div ref={bottomRef} />
        </div>
      </ScrollArea>

      <ChatInput onSend={onSend} disabled={!connected} />
    </div>
  )
}
