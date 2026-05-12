// chat-layout.tsx
// Requires: shadcn/ui, lucide-react, tailwindcss
// shadcn components used: Avatar, Badge, Button, ScrollArea, Separator, Tooltip, Input

import { useState, useRef, useEffect } from "react"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Separator } from "@/components/ui/separator"
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip"
import {
  Search,
  Settings,
  Plus,
  MoreHorizontal,
  Send,
  Paperclip,
  Smile,
  Circle,
} from "lucide-react"
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
  SheetTrigger,
} from "@/components/ui/sheet"
import { baseip } from "@/encryption/ip"
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
  onCreateConversation,
}: {
  currentUser: User
  conversations: Conversation[]
  activeId: string
  onSelect: (id: string) => void
  onCreateConversation: (
    name: string,
    participantEmails: string
  ) => Promise<void>
}) {
  const [search, setSearch] = useState("")
  const [sheetOpen, setSheetOpen] = useState(false)
  const [newConversationName, setNewConversationName] = useState("")
  const [participantEmails, setParticipantEmails] = useState("")
  const [creating, setCreating] = useState(false)
  const [createError, setCreateError] = useState<string | null>(null)

  const handleCreateConversation = async () => {
    setCreateError(null)
    setCreating(true)

    try {
      await onCreateConversation(newConversationName, participantEmails)
      setNewConversationName("")
      setParticipantEmails("")
      setSheetOpen(false)
    } catch (err: unknown) {
      setCreateError(
        err instanceof Error ? err.message : "Failed to create conversation"
      )
    } finally {
      setCreating(false)
    }
  }

  const filtered = conversations.filter((c) =>
    c.name.toLowerCase().includes(search.toLowerCase())
  )

  return (
    <aside className="flex h-screen w-72 shrink-0 flex-col border-r bg-background">
      {/* Header */}
      <div className="flex items-center justify-between px-4 pt-5 pb-3">
        <h1 className="text-lg font-semibold tracking-tight">Messages</h1>
        <Sheet open={sheetOpen} onOpenChange={setSheetOpen}>
          <SheetTrigger asChild>
            <Button variant="ghost" size="icon" className="h-8 w-8">
              <Plus className="h-4 w-4" />
            </Button>
          </SheetTrigger>
          <SheetContent side="right" className="w-[400px]">
            <SheetHeader>
              <SheetTitle>New conversation</SheetTitle>
              <SheetDescription>
                Create a direct or group chat by adding participant emails.
              </SheetDescription>
            </SheetHeader>
            <div className="space-y-4 p-4">
              <div className="space-y-2">
                <Label htmlFor="conversation-name">Conversation name</Label>
                <Input
                  id="conversation-name"
                  placeholder="Project team, Friends"
                  value={newConversationName}
                  onChange={(e) => setNewConversationName(e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="participant-emails">Participant emails</Label>
                <Input
                  id="participant-emails"
                  placeholder="alice@example.com, bob@example.com"
                  value={participantEmails}
                  onChange={(e) => setParticipantEmails(e.target.value)}
                />
                <p className="text-sm text-muted-foreground">
                  Enter one or more emails separated by commas.
                </p>
              </div>
              {createError ? (
                <p className="text-sm text-red-600">{createError}</p>
              ) : null}
              <Button
                className="w-full"
                onClick={handleCreateConversation}
                disabled={creating}
              >
                {creating ? "Creating..." : "Create conversation"}
              </Button>
            </div>
          </SheetContent>
        </Sheet>
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

export function ChatLayout({
  currentUser,
  conversations,
}: {
  currentUser: User
  conversations: Conversation[]
}) {
  const [conversationList, setConversationList] = useState(conversations)
  const [activeId, setActiveId] = useState(conversations[0]?.id ?? "")
  const [messagesByConv, setMessagesByConv] = useState<
    Record<string, Message[]>
  >(() =>
    conversations.reduce(
      (acc, conversation) => {
        acc[conversation.id] = []
        return acc
      },
      {} as Record<string, Message[]>
    )
  )
  useEffect(() => {
    setConversationList(conversations)
    if (
      conversations.length > 0 &&
      !conversations.some((c) => c.id === activeId)
    ) {
      setActiveId(conversations[0].id)
    }
  }, [conversations])

  useEffect(() => {
    setMessagesByConv((prev) => {
      const next = { ...prev }
      conversationList.forEach((conversation) => {
        if (!(conversation.id in next)) {
          next[conversation.id] = []
        }
      })
      return next
    })
  }, [conversationList])

  const activeConversation =
    conversationList.find((conversation) => conversation.id === activeId) ??
    conversationList[0]

  const handleCreateConversation = async (
    name: string,
    participants: string
  ) => {
    const trimmedName = name.trim()
    if (!trimmedName) {
      throw new Error("Conversation name is required")
    }

    const participantEmails = participants
      .split(",")
      .map((email) => email.trim())
      .filter(Boolean)

    if (participantEmails.length === 0) {
      throw new Error("Add at least one participant email")
    }

    const token = localStorage.getItem("accessToken")
    if (!token) {
      throw new Error("Not authenticated")
    }

    const participantUsers = await Promise.all(
      participantEmails.map(async (email) => {
        const res = await fetch(
          `${baseip}/users/search?email=${encodeURIComponent(email)}`,
          {
            headers: {
              Authorization: `Bearer ${token}`,
              "Content-Type": "application/json",
            },
          }
        )

        if (!res.ok) {
          throw new Error(`User not found: ${email}`)
        }

        return res.json() as Promise<{ id: number }>
      })
    )

    const participantKeys: Record<string, string> = {
      [currentUser.id]: "dummy-key",
    }

    participantUsers.forEach((user) => {
      participantKeys[String(user.id)] = "dummy-key"
    })

    const createRes = await fetch(`${baseip}/conversations`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        name: trimmedName,
        participantKeys,
      }),
    })

    if (!createRes.ok) {
      const data = await createRes.json().catch(() => null)
      const message =
        data && typeof data === "object" && "detail" in data
          ? (data as any).detail
          : "Failed to create conversation"
      throw new Error(message)
    }

    const body = (await createRes.json()) as { id: number }
    const newConversation: Conversation = {
      id: String(body.id),
      name: trimmedName,
      lastMessage: "",
      lastMessageTime: new Date(),
      unreadCount: 0,
    }

    setConversationList((current) => [...current, newConversation])
    setActiveId(String(body.id))
  }

  const activeMessages =
    activeConversation && messagesByConv[activeConversation.id]
      ? messagesByConv[activeConversation.id]
      : []

  const handleSend = (text: string) => {
    if (!activeConversation) return
    const newMsg: Message = {
      id: crypto.randomUUID(),
      senderId: currentUser.id,
      text,
      timestamp: new Date(),
      isOwn: true,
    }
    setMessagesByConv((prev) => ({
      ...prev,
      [activeConversation.id]: [...(prev[activeConversation.id] ?? []), newMsg],
    }))
  }

  const allUsers: Record<string, User> = {
    [currentUser.id]: currentUser,
  }

  return (
    <div className="flex h-screen overflow-hidden bg-background text-foreground">
      <Sidebar
        currentUser={currentUser}
        conversations={conversationList}
        activeId={activeConversation?.id ?? ""}
        onSelect={setActiveId}
        onCreateConversation={handleCreateConversation}
      />

      {activeConversation ? (
        <ChatArea
          conversation={activeConversation}
          messages={activeMessages}
          users={allUsers}
          onSend={handleSend}
        />
      ) : (
        <div className="flex flex-1 flex-col items-center justify-center p-8 text-center">
          <div className="rounded-2xl border bg-card p-8 shadow-lg">
            <h2 className="text-xl font-semibold">No conversations yet</h2>
            <p className="mt-2 text-sm text-muted-foreground">
              Create your first conversation using the plus button in the sidebar.
            </p>
          </div>
        </div>
      )}
    </div>
  )
}

export default ChatLayout
