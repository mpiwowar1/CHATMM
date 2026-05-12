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

function Sidebar({
  currentUser,
  conversations,
  activeId,
  onSelect,
  onNewConversation,
}: {
  currentUser: UserResponse
  conversations: ConversationSummaryResponse[]
  activeId: number | null
  onSelect: (id: number) => void
  onNewConversation?: () => void
}) {
  return (
    <aside className="flex h-screen w-72 shrink-0 flex-col border-r bg-background">
      <div className="flex items-center justify-between px-4 pt-5 pb-3">
        <h1 className="text-lg font-semibold tracking-tight">Conversations</h1>
        <TooltipProvider delayDuration={300}>
          <Tooltip>
            <TooltipTrigger asChild>
              <Button
                variant="ghost"
                size="icon"
                className="h-8 w-8"
                onClick={onNewConversation}
              >
                <Plus className="h-4 w-4" />
              </Button>
            </TooltipTrigger>
            <TooltipContent>New conversation</TooltipContent>
          </Tooltip>
        </TooltipProvider>
      </div>

      <Separator />

      <ScrollArea className="flex-1 px-2 py-2">
        <div className="flex flex-col gap-0.5">
          {conversations.map((c) => (
            <ConversationItem
              key={c.id}
              conversation={c}
              isActive={c.id === activeId}
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
