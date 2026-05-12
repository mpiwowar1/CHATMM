import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Badge } from "@/components/ui/badge"
import { User, Users } from "lucide-react"
import { cn } from "@/lib/utils"
import type { ConversationSummaryResponse } from "./chat-types"

function ConversationItem({
  conversation,
  isActive,
  onClick,
}: {
  conversation: ConversationSummaryResponse
  isActive: boolean
  onClick: () => void
}) {
  const displayName =
    conversation.name ||
    (conversation.type === "GROUP" ? "Group Chat" : "Direct Message")

  return (
    <button
      onClick={onClick}
      className={cn(
        "flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left transition-colors",
        isActive ? "bg-accent text-accent-foreground" : "hover:bg-accent/50"
      )}
    >
      <Avatar className="h-9 w-9 shrink-0">
        <AvatarFallback className="text-xs font-semibold">
          {displayName.slice(0, 2).toUpperCase()}
        </AvatarFallback>
      </Avatar>

      <div className="flex min-w-0 flex-1 items-center justify-between gap-2">
        <span className="truncate text-sm font-medium">{displayName}</span>
        <Badge
          variant={conversation.type === "GROUP" ? "secondary" : "outline"}
          className="flex h-4 shrink-0 items-center gap-0.5 px-1.5 text-[10px]"
        >
          {conversation.type === "GROUP" ? (
            <>
              <Users className="h-2.5 w-2.5" /> Group
            </>
          ) : (
            <>
              <User className="h-2.5 w-2.5" /> DM
            </>
          )}
        </Badge>
      </div>
    </button>
  )
}

export default ConversationItem
