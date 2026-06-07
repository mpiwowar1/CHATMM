import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Button } from "@/components/ui/button"
import { MoreHorizontal } from "lucide-react"
import type { ConversationSummaryResponse } from "./chat-types"

function ChatHeader({
  conversation,
  onMore,
}: {
  conversation: ConversationSummaryResponse
  onMore?: () => void
}) {
  const displayName =
    conversation.name ||
    (conversation.type === "GROUP" ? "Group Chat" : "Direct Message")

  return (
    <div className="flex shrink-0 items-center justify-between border-b bg-background px-5 py-3.5">
      <div className="flex items-center gap-3">
        <Avatar className="h-9 w-9">
          <AvatarFallback className="text-xs font-semibold">
            {displayName.slice(0, 2).toUpperCase()}
          </AvatarFallback>
        </Avatar>

        <div>
          <h2 className="text-sm leading-none font-semibold">{displayName}</h2>
          <p className="mt-1 text-xs text-muted-foreground">
            {conversation.type === "GROUP"
              ? "Group conversation"
              : "Direct message"}
          </p>
        </div>
      </div>

      {/* <Button variant="ghost" size="icon" className="h-8 w-8" onClick={onMore}>
        <MoreHorizontal className="h-4 w-4" />
      </Button> */}
    </div>
  )
}

export default ChatHeader
