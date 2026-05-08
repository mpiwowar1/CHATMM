// chat-layout.tsx
// Requires: shadcn/ui, lucide-react, tailwindcss
// shadcn components used: Avatar, Badge, Button, ScrollArea, Separator, Tooltip, Input

import { useState, useRef, useEffect } from "react"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Separator } from "@/components/ui/separator"
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip"
import {
  MessageSquare,
  Search,
  Settings,
  Plus,
  MoreHorizontal,
  Send,
  Paperclip,
  Smile,
  ChevronDown,
  Circle,
} from "lucide-react"
import { cn } from "@/lib/utils"

// ─────────────────────────────────────────────
// Types
// ─────────────────────────────────────────────

export interface User {
  id: string
  name: string
  avatar?: string
  status: "online" | "away" | "offline"
  role?: string
}

export interface Message {
  id: string
  senderId: string
  text: string
  timestamp: Date
  isOwn: boolean
}

export interface Conversation {
  id: string
  name: string
  avatar?: string
  lastMessage: string
  lastMessageTime: Date
  unreadCount?: number
  isGroup?: boolean
  participants?: User[]
}

// ─────────────────────────────────────────────
// StatusDot — small colored circle
// ─────────────────────────────────────────────

function StatusDot({ status }: { status: User["status"] }) {
  return (
    <span
      className={cn(
        "absolute right-0 bottom-0 block h-2.5 w-2.5 rounded-full ring-2 ring-background",
        status === "online" && "bg-emerald-500",
        status === "away" && "bg-amber-400",
        status === "offline" && "bg-muted-foreground"
      )}
    />
  )
}

// ─────────────────────────────────────────────
// SidebarUserBlock — current user at the bottom
// ─────────────────────────────────────────────

export function SidebarUserBlock({ user }: { user: User }) {
  return (
    <div className="group flex cursor-pointer items-center gap-3 rounded-lg px-3 py-3 transition-colors hover:bg-accent">
      <div className="relative shrink-0">
        <Avatar className="h-9 w-9">
          <AvatarImage src={user.avatar} alt={user.name} />
          <AvatarFallback className="text-xs font-semibold">
            {user.name.slice(0, 2).toUpperCase()}
          </AvatarFallback>
        </Avatar>
        <StatusDot status={user.status} />
      </div>

      <div className="min-w-0 flex-1">
        <p className="truncate text-sm leading-none font-medium">{user.name}</p>
        <p className="mt-0.5 text-xs text-muted-foreground capitalize">
          {user.status}
        </p>
      </div>

      <TooltipProvider delayDuration={300}>
        <Tooltip>
          <TooltipTrigger asChild>
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7 shrink-0 opacity-0 transition-opacity group-hover:opacity-100"
            >
              <Settings className="h-3.5 w-3.5" />
            </Button>
          </TooltipTrigger>
          <TooltipContent side="right">Settings</TooltipContent>
        </Tooltip>
      </TooltipProvider>
    </div>
  )
}

// ─────────────────────────────────────────────
// ConversationItem — single row in the sidebar list
// ─────────────────────────────────────────────

function ConversationItem({
  conversation,
  isActive,
  onClick,
}: {
  conversation: Conversation
  isActive: boolean
  onClick: () => void
}) {
  const timeStr = conversation.lastMessageTime.toLocaleTimeString([], {
    hour: "2-digit",
    minute: "2-digit",
  })

  return (
    <button
      onClick={onClick}
      className={cn(
        "flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left transition-colors",
        isActive
          ? "bg-accent text-accent-foreground"
          : "text-foreground hover:bg-accent/50"
      )}
    >
      <div className="relative shrink-0">
        <Avatar className="h-10 w-10">
          <AvatarImage src={conversation.avatar} alt={conversation.name} />
          <AvatarFallback className="text-xs font-semibold">
            {conversation.name.slice(0, 2).toUpperCase()}
          </AvatarFallback>
        </Avatar>
      </div>

      <div className="min-w-0 flex-1">
        <div className="flex items-center justify-between gap-1">
          <span className="truncate text-sm font-medium">
            {conversation.name}
          </span>
          <span className="shrink-0 text-[11px] text-muted-foreground">
            {timeStr}
          </span>
        </div>
        <div className="mt-0.5 flex items-center justify-between gap-1">
          <p className="truncate text-xs text-muted-foreground">
            {conversation.lastMessage}
          </p>
          {conversation.unreadCount ? (
            <Badge
              variant="default"
              className="h-4 min-w-4 shrink-0 rounded-full px-1 text-[10px]"
            >
              {conversation.unreadCount}
            </Badge>
          ) : null}
        </div>
      </div>
    </button>
  )
}

// ─────────────────────────────────────────────
// Sidebar — full left panel
// ─────────────────────────────────────────────

export function Sidebar({
  currentUser,
  conversations,
  activeId,
  onSelect,
}: {
  currentUser: User
  conversations: Conversation[]
  activeId: string
  onSelect: (id: string) => void
}) {
  const [search, setSearch] = useState("")

  const filtered = conversations.filter((c) =>
    c.name.toLowerCase().includes(search.toLowerCase())
  )

  return (
    <aside className="flex h-screen w-72 shrink-0 flex-col border-r bg-background">
      {/* Header */}
      <div className="flex items-center justify-between px-4 pt-5 pb-3">
        <h1 className="text-lg font-semibold tracking-tight">Messages</h1>
        <Button variant="ghost" size="icon" className="h-8 w-8">
          <Plus className="h-4 w-4" />
        </Button>
      </div>

      {/* Search */}
      <div className="px-3 pb-3">
        <div className="relative">
          <Search className="absolute top-2.5 left-2.5 h-3.5 w-3.5 text-muted-foreground" />
          <Input
            placeholder="Search conversations…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="h-8 border-0 bg-muted pl-8 text-sm focus-visible:ring-1"
          />
        </div>
      </div>

      <Separator />

      {/* Conversation list */}
      <ScrollArea className="flex-1 px-2 py-2">
        <div className="flex flex-col gap-0.5">
          {filtered.map((c) => (
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

      {/* Current user block */}
      <div className="px-2 py-2">
        <SidebarUserBlock user={currentUser} />
      </div>
    </aside>
  )
}

// ─────────────────────────────────────────────
// MessageBubble — individual chat message
// ─────────────────────────────────────────────

export function MessageBubble({
  message,
  sender,
  showAvatar,
}: {
  message: Message
  sender: User
  showAvatar: boolean
}) {
  const timeStr = message.timestamp.toLocaleTimeString([], {
    hour: "2-digit",
    minute: "2-digit",
  })

  if (message.isOwn) {
    return (
      <div className="group flex items-end justify-end gap-2">
        <span className="mb-1 text-[10px] text-muted-foreground opacity-0 transition-opacity group-hover:opacity-100">
          {timeStr}
        </span>
        <div className="max-w-[65%] rounded-2xl rounded-br-sm bg-primary px-4 py-2 text-sm leading-relaxed text-primary-foreground shadow-sm">
          {message.text}
        </div>
      </div>
    )
  }

  return (
    <div className="group flex items-end gap-2">
      <div className="w-7 shrink-0">
        {showAvatar && (
          <Avatar className="h-7 w-7">
            <AvatarImage src={sender.avatar} alt={sender.name} />
            <AvatarFallback className="text-[10px]">
              {sender.name.slice(0, 2).toUpperCase()}
            </AvatarFallback>
          </Avatar>
        )}
      </div>

      <div className="max-w-[65%]">
        {showAvatar && (
          <p className="mb-1 ml-1 text-xs font-medium text-muted-foreground">
            {sender.name}
          </p>
        )}
        <div className="rounded-2xl rounded-bl-sm bg-muted px-4 py-2 text-sm leading-relaxed shadow-sm">
          {message.text}
        </div>
      </div>

      <span className="mb-1 text-[10px] text-muted-foreground opacity-0 transition-opacity group-hover:opacity-100">
        {timeStr}
      </span>
    </div>
  )
}

// ─────────────────────────────────────────────
// ChatHeader — name bar at the top of chat area
// ─────────────────────────────────────────────

export function ChatHeader({ conversation }: { conversation: Conversation }) {
  return (
    <div className="flex shrink-0 items-center justify-between border-b bg-background px-5 py-3.5">
      <div className="flex items-center gap-3">
        <div className="relative">
          <Avatar className="h-9 w-9">
            <AvatarImage src={conversation.avatar} alt={conversation.name} />
            <AvatarFallback className="text-xs font-semibold">
              {conversation.name.slice(0, 2).toUpperCase()}
            </AvatarFallback>
          </Avatar>
          <span className="absolute right-0 bottom-0 block h-2.5 w-2.5 rounded-full bg-emerald-500 ring-2 ring-background" />
        </div>

        <div>
          <h2 className="text-sm leading-none font-semibold">
            {conversation.name}
          </h2>
          <p className="mt-1 flex items-center gap-1 text-xs text-muted-foreground">
            <Circle className="h-1.5 w-1.5 fill-emerald-500 text-emerald-500" />
            Active now
          </p>
        </div>
      </div>

      <Button variant="ghost" size="icon" className="h-8 w-8">
        <MoreHorizontal className="h-4 w-4" />
      </Button>
    </div>
  )
}

// ─────────────────────────────────────────────
// ChatInput — message composer at the bottom
// ─────────────────────────────────────────────

export function ChatInput({ onSend }: { onSend: (text: string) => void }) {
  const [value, setValue] = useState("")

  const handleSend = () => {
    const trimmed = value.trim()
    if (!trimmed) return
    onSend(trimmed)
    setValue("")
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  return (
    <div className="shrink-0 border-t bg-background px-4 py-3">
      <div className="flex items-center gap-2 rounded-xl bg-muted px-3 py-1.5">
        <TooltipProvider delayDuration={300}>
          <Tooltip>
            <TooltipTrigger asChild>
              <Button
                variant="ghost"
                size="icon"
                className="h-8 w-8 shrink-0 text-muted-foreground hover:text-foreground"
              >
                <Paperclip className="h-4 w-4" />
              </Button>
            </TooltipTrigger>
            <TooltipContent>Attach file</TooltipContent>
          </Tooltip>
        </TooltipProvider>

        <Input
          value={value}
          onChange={(e) => setValue(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Type a message…"
          className="h-8 flex-1 border-0 bg-transparent px-0 text-sm shadow-none focus-visible:ring-0"
        />

        <TooltipProvider delayDuration={300}>
          <Tooltip>
            <TooltipTrigger asChild>
              <Button
                variant="ghost"
                size="icon"
                className="h-8 w-8 shrink-0 text-muted-foreground hover:text-foreground"
              >
                <Smile className="h-4 w-4" />
              </Button>
            </TooltipTrigger>
            <TooltipContent>Emoji</TooltipContent>
          </Tooltip>
        </TooltipProvider>

        <Button
          size="icon"
          className="h-8 w-8 shrink-0 rounded-lg"
          onClick={handleSend}
          disabled={!value.trim()}
        >
          <Send className="h-3.5 w-3.5" />
        </Button>
      </div>
    </div>
  )
}

// ─────────────────────────────────────────────
// ChatArea — scrollable messages + input
// ─────────────────────────────────────────────

export function ChatArea({
  conversation,
  messages,
  users,
  onSend,
}: {
  conversation: Conversation
  messages: Message[]
  users: Record<string, User>
  onSend: (text: string) => void
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
            const sender = users[msg.senderId] ?? {
              id: msg.senderId,
              name: "Unknown",
              status: "offline" as const,
            }
            const prev = messages[idx - 1]
            const showAvatar = !prev || prev.senderId !== msg.senderId

            return (
              <MessageBubble
                key={msg.id}
                message={msg}
                sender={sender}
                showAvatar={showAvatar}
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

// ─────────────────────────────────────────────
// ChatLayout — root layout combining both panels
// ─────────────────────────────────────────────

export function ChatLayout() {
  // ── Sample data ──────────────────────────────
  const currentUser: User = {
    id: "me",
    name: "Alex Morgan",
    status: "online",
    role: "Developer",
  }

  const otherUsers: Record<string, User> = {
    u1: { id: "u1", name: "Sara Kim", status: "online" },
    u2: { id: "u2", name: "James Lee", status: "away" },
    u3: { id: "u3", name: "Priya Das", status: "online" },
    u4: { id: "u4", name: "Tom Blake", status: "offline" },
  }

  const allUsers: Record<string, User> = {
    me: currentUser,
    ...otherUsers,
  }

  const conversations: Conversation[] = [
    {
      id: "c1",
      name: "Sara Kim",
      lastMessage: "Sounds great, talk later!",
      lastMessageTime: new Date(Date.now() - 3 * 60000),
      unreadCount: 2,
    },
    {
      id: "c2",
      name: "James Lee",
      lastMessage: "Can you review the PR?",
      lastMessageTime: new Date(Date.now() - 22 * 60000),
      unreadCount: 0,
    },
    {
      id: "c3",
      name: "Priya Das",
      lastMessage: "The meeting is at 3pm",
      lastMessageTime: new Date(Date.now() - 60 * 60000),
      unreadCount: 0,
    },
    {
      id: "c4",
      name: "Tom Blake",
      lastMessage: "Thanks!",
      lastMessageTime: new Date(Date.now() - 5 * 3600000),
      unreadCount: 0,
    },
  ]

  const sampleMessages: Message[] = [
    {
      id: "m1",
      senderId: "u1",
      text: "Hey! Did you finish the design review?",
      timestamp: new Date(Date.now() - 15 * 60000),
      isOwn: false,
    },
    {
      id: "m2",
      senderId: "me",
      text: "Almost done, just checking spacing on mobile.",
      timestamp: new Date(Date.now() - 14 * 60000),
      isOwn: true,
    },
    {
      id: "m3",
      senderId: "u1",
      text: "Nice. Let me know when you push it.",
      timestamp: new Date(Date.now() - 13 * 60000),
      isOwn: false,
    },
    {
      id: "m4",
      senderId: "u1",
      text: "Also, the shadcn components look really clean 👌",
      timestamp: new Date(Date.now() - 12 * 60000),
      isOwn: false,
    },
    {
      id: "m5",
      senderId: "me",
      text: "Thanks! Used the new ScrollArea and Separator — super easy.",
      timestamp: new Date(Date.now() - 10 * 60000),
      isOwn: true,
    },
    {
      id: "m6",
      senderId: "u1",
      text: "Sounds great, talk later!",
      timestamp: new Date(Date.now() - 3 * 60000),
      isOwn: false,
    },
  ]

  // ── State ─────────────────────────────────────
  const [activeId, setActiveId] = useState("c1")
  const [messagesByConv, setMessagesByConv] = useState<
    Record<string, Message[]>
  >({
    c1: sampleMessages,
    c2: [],
    c3: [],
    c4: [],
  })

  const activeConversation = conversations.find((c) => c.id === activeId)!
  const activeMessages = messagesByConv[activeId] ?? []

  const handleSend = (text: string) => {
    const newMsg: Message = {
      id: crypto.randomUUID(),
      senderId: "me",
      text,
      timestamp: new Date(),
      isOwn: true,
    }
    setMessagesByConv((prev) => ({
      ...prev,
      [activeId]: [...(prev[activeId] ?? []), newMsg],
    }))
  }

  return (
    <div className="flex h-screen overflow-hidden bg-background text-foreground">
      <Sidebar
        currentUser={currentUser}
        conversations={conversations}
        activeId={activeId}
        onSelect={setActiveId}
      />

      <ChatArea
        conversation={activeConversation}
        messages={activeMessages}
        users={allUsers}
        onSend={handleSend}
      />
    </div>
  )
}

export default ChatLayout
