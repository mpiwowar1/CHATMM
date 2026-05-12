import { useEffect, useRef } from "react"
import { ScrollArea } from "@/components/ui/scroll-area"
import type {
  ChatMessageResponse,
  ConversationSummaryResponse,
} from "./chat-types"
import ChatHeader from "./ChatHeader"
import { ChatInput } from "./ChatInput"
import { MessageBubble } from "./MessageBubble"

export function ChatArea({
  currentUserId,
  conversation,
  messages,
  decryptedMap,
  onSend,
}: {
  currentUserId: number
  conversation: ConversationSummaryResponse
  messages: ChatMessageResponse[]
  decryptedMap: Record<number, string>
  onSend: (plaintext: string) => void
}) {
  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [messages])

  return (
    <div className="flex h-screen min-w-0 flex-1 flex-col">
      <ChatHeader conversation={conversation} />

      <ScrollArea className="flex-1 px-5 py-4">
        <div className="flex flex-col gap-3">
          {messages.map((msg, idx) => {
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
                decryptedText={decryptedMap[msg.id] ?? "…"}
              />
            )
          })}
          <div ref={bottomRef} />
        </div>
      </ScrollArea>

      <ChatInput onSend={onSend} />
    </div>
  )
}
