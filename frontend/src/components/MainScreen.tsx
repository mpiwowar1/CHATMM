import { useEffect, useState } from "react"
import ChatLayout from "./chat-layout"
import type { Conversation } from "./chat-layout"
import { baseip } from "../encryption/ip"

interface BackendConversation {
  id: number
  name: string
  type: string
  encryptedAesKey: string
}

export default function MainScreen() {
  const [conversations, setConversations] = useState<Conversation[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const token = localStorage.getItem("accessToken")
    if (!token) {
      setError("Not authenticated")
      setLoading(false)
      return
    }

    fetch(baseip + "/conversations", {
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
    })
      .then(async (res) => {
        if (!res.ok) {
          const data = await res.json().catch(() => null)
          const message =
            data && typeof data === "object" && "detail" in data
              ? (data as any).detail
              : "Failed to load conversations"
          throw new Error(message)
        }

        return res.json() as Promise<BackendConversation[]>
      })
      .then((backendConversations) => {
        setConversations(
          backendConversations.map((conversation) => ({
            id: String(conversation.id),
            name: conversation.name,
            lastMessage: "",
            lastMessageTime: new Date(),
            unreadCount: 0,
          }))
        )
      })
      .catch((err: unknown) => {
        setError(
          err instanceof Error ? err.message : "Failed to load conversations"
        )
      })
      .finally(() => {
        setLoading(false)
      })
  }, [])

  const storedUser = localStorage.getItem("user")
  const parsedUser = storedUser ? JSON.parse(storedUser) : null
  const currentUser = {
    id: parsedUser?.id ? String(parsedUser.id) : "me",
    name: parsedUser?.name ?? "Me",
    status: parsedUser?.status ?? "online",
  }

  if (loading) {
    return (
      <div className="flex h-screen items-center justify-center bg-background text-foreground">
        <p className="text-base">Loading conversations…</p>
      </div>
    )
  }

  if (error) {
    return (
      <div className="flex h-screen items-center justify-center bg-background text-foreground">
        <div className="rounded-2xl border bg-card p-8 text-center shadow-lg">
          <h2 className="text-xl font-semibold">
            Unable to load conversations
          </h2>
          <p className="mt-2 text-sm text-muted-foreground">{error}</p>
        </div>
      </div>
    )
  }

  return <ChatLayout currentUser={currentUser} conversations={conversations} />
}
