import { Button } from "@/components/ui/button"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Separator } from "@/components/ui/separator"
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip"
import { Plus } from "lucide-react"
import type { ConversationSummaryResponse, UserResponse } from "./chat-types"
import ConversationItem from "./ConversationItem"
import SidebarUserBlock from "./SidebarUserBlock"
import NewConversationSheet from "./NewConversationSheet"

function Sidebar({
  currentUser,
  conversations,
  decryptedPreviews,
  activeId,
  onSelect,
  onCreateConversation,
}: {
  currentUser: UserResponse
  conversations: ConversationSummaryResponse[]
  activeId: number | null
  decryptedPreviews: Record<number, string>
  onSelect: (id: number) => void
  onCreateConversation?: (
    name: string | null,
    participants: ParticipantKey[]
  ) => Promise<number | null>
}) {
  return (
    <aside className="flex h-screen w-72 shrink-0 flex-col border-r bg-background">
      <div className="flex items-center justify-between px-4 pt-5 pb-3">
        <h1 className="text-lg font-semibold tracking-tight">Conversations</h1>
        <div>
          <NewConversationSheet
            onCreate={async (name, participants) => {
              if (!onCreateConversation) return null
              return await onCreateConversation(name, participants)
            }}
          />
        </div>
      </div>

      <Separator />

      <ScrollArea className="flex-1 px-2 py-2">
        <div className="flex flex-col gap-0.5">
          {conversations.map((c) => (
            <ConversationItem
              key={c.id}
              conversation={c}
              isActive={c.id === activeId}
              preview={decryptedPreviews[c.id] ?? null}
              onClick={() => onSelect(c.id)}
            />
          ))}
        </div>
      </ScrollArea>

      <Separator />

      <div className="px-2 py-2">
        <SidebarUserBlock user={currentUser} />
      </div>
    </aside>
  )
}

export default Sidebar
