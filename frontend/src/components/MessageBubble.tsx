import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import type { DecryptedMessage } from "@/hooks/useChat"

/** Message bubble showing sender, time and decrypted text. */
export function MessageBubble({
  message,
  isOwn,
  showSenderName,
}: {
  message: DecryptedMessage
  isOwn: boolean
  showSenderName: boolean
}) {
  const timeStr = new Date(message.timestamp).toLocaleTimeString([], {
    hour: "2-digit",
    minute: "2-digit",
  })

  if (isOwn) {
    return (
      <div className="group flex items-end justify-end gap-2">
        <span className="mb-1 text-[10px] text-muted-foreground opacity-0 transition-opacity group-hover:opacity-100">
          {timeStr}
        </span>
        <div className="max-w-[65%] rounded-2xl rounded-br-sm bg-primary px-4 py-2 text-sm leading-relaxed text-primary-foreground shadow-sm">
          {message.failed ? (
            <span className="italic opacity-70">[decryption failed]</span>
          ) : (
            message.text
          )}
        </div>
      </div>
    )
  }

  return (
    <div className="group flex items-end gap-2">
      <Avatar className="h-7 w-7 shrink-0">
        <AvatarFallback className="text-[10px]">
          {message.senderName.slice(0, 2).toUpperCase()}
        </AvatarFallback>
      </Avatar>

      <div className="max-w-[65%]">
        {showSenderName && (
          <p className="mb-1 ml-1 text-xs font-medium text-muted-foreground">
            {message.senderName}
          </p>
        )}
        <div className="rounded-2xl rounded-bl-sm bg-muted px-4 py-2 text-sm leading-relaxed shadow-sm">
          {message.failed ? (
            <span className="italic opacity-70">[decryption failed]</span>
          ) : (
            message.text
          )}
        </div>
      </div>

      <span className="mb-1 text-[10px] text-muted-foreground opacity-0 transition-opacity group-hover:opacity-100">
        {timeStr}
      </span>
    </div>
  )
}
