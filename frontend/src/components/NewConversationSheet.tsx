import { useState } from "react"
import {
  Sheet,
  SheetTrigger,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
  SheetFooter,
} from "@/components/ui/sheet"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Check, X } from "lucide-react"
import useUserSearch from "@/hooks/useUserSearch"
import { baseip } from "@/encryption/ip"
import type { ParticipantKey } from "@/encryption/conversationCrypto"

// Full user shape returned by /users/search
type SelectedUser = {
  id: number
  email: string
  name: string
  publicKey: string
}

function getAuthHeader(): Record<string, string> {
  const token = localStorage.getItem("accessToken")
  return token ? { Authorization: `Bearer ${token}` } : {}
}

async function fetchFullUser(email: string): Promise<SelectedUser | null> {
  const res = await fetch(
    `${baseip}/users/search?email=${encodeURIComponent(email)}`,
    {
      headers: { "Content-Type": "application/json", ...getAuthHeader() },
    }
  )
  if (!res.ok) return null
  return res.json() // { id, email, name, publicKey }
}

export default function NewConversationSheet({
  onCreate,
}: {
  onCreate: (
    name: string | null,
    participants: ParticipantKey[]
  ) => Promise<number | null>
}) {
  const [open, setOpen] = useState(false)
  const [query, setQuery] = useState("")
  const [selected, setSelected] = useState<SelectedUser[]>([])
  const [name, setName] = useState<string | null>(null)
  const [resolving, setResolving] = useState(false)

  const { results, loading } = useUserSearch(query, 8, open)

  const isSelected = (email: string) => selected.some((u) => u.email === email)

  const toggle = async (email: string) => {
    if (isSelected(email)) {
      setSelected((prev) => prev.filter((u) => u.email !== email))
      return
    }
    setResolving(true)
    try {
      const user = await fetchFullUser(email)
      if (!user) {
        alert(`Could not resolve user: ${email}`)
        return
      }
      setSelected((prev) => [...prev, user])
    } finally {
      setResolving(false)
    }
  }

  const handleCreate = async () => {
    if (selected.length === 0) {
      alert("Select at least one user")
      return
    }

    // Pass only id + publicKey — useConversations adds the creator automatically
    const participants: ParticipantKey[] = selected.map(
      ({ id, publicKey }) => ({ id, publicKey })
    )
    const newId = await onCreate(name, participants)

    if (newId) {
      setOpen(false)
      setQuery("")
      setSelected([])
      setName(null)
    }
  }

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger asChild>
        <Button variant="ghost" size="icon" className="h-8 w-8">
          <span className="sr-only">New conversation</span>+
        </Button>
      </SheetTrigger>
      <SheetContent side="right">
        <SheetHeader>
          <SheetTitle>Create conversation</SheetTitle>
          <SheetDescription>
            Find users and add them to the conversation
          </SheetDescription>
        </SheetHeader>

        <div className="p-4">
          <label className="block text-sm text-muted-foreground">
            Name (optional)
          </label>
          <Input
            value={name ?? ""}
            onChange={(e) => setName(e.target.value || null)}
            className="mb-3"
          />

          {selected.length > 0 && (
            <div className="mb-3 flex flex-wrap gap-1">
              {selected.map((u) => (
                <span
                  key={u.id}
                  className="flex items-center gap-1 rounded-full bg-accent px-2 py-0.5 text-xs"
                >
                  {u.name}
                  <button
                    onClick={() =>
                      setSelected((prev) => prev.filter((s) => s.id !== u.id))
                    }
                  >
                    <X className="h-3 w-3" />
                  </button>
                </span>
              ))}
            </div>
          )}

          <label className="block text-sm text-muted-foreground">
            Search users
          </label>
          <Input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search by email"
          />

          <div className="mt-3 flex max-h-64 flex-col gap-2 overflow-auto">
            {(loading || resolving) && (
              <div className="text-sm text-muted-foreground">
                {resolving ? "Resolving user…" : "Searching…"}
              </div>
            )}
            {results.map((u) => (
              <button
                key={u.email}
                onClick={() => toggle(u.email)}
                className="flex items-center gap-3 rounded px-2 py-2 hover:bg-accent/50"
              >
                <Avatar className="h-8 w-8">
                  <AvatarFallback>
                    {u.name.slice(0, 2).toUpperCase()}
                  </AvatarFallback>
                </Avatar>
                <div className="flex-1 text-left">
                  <div className="text-sm font-medium">{u.name}</div>
                  <div className="text-xs text-muted-foreground">{u.email}</div>
                </div>
                {isSelected(u.email) && (
                  <Check className="h-4 w-4 text-green-600" />
                )}
              </button>
            ))}
          </div>
        </div>

        <SheetFooter>
          <div className="flex gap-2">
            <Button variant="ghost" onClick={() => setOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleCreate}>Create</Button>
          </div>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  )
}
