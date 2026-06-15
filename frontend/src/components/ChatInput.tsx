import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Send } from "lucide-react"

/** Simple text input with send button for chat messages. */
export function ChatInput({ onSend }: { onSend: (plaintext: string) => void }) {
  const [value, setValue] = useState("")

  const handleSend = () => {
    const trimmed = value.trim().slice(0, 2000)
    if (!trimmed) return
    onSend(trimmed)
    setValue("")
  }

  return (
    <div className="shrink-0 border-t bg-background px-4 py-3">
      <div className="flex items-center gap-2 rounded-xl bg-muted px-3 py-1.5">
        <Input
          value={value}
          maxLength={2000}
          onChange={(e) => setValue(e.target.value.slice(0, 2000))}
          onKeyDown={(e) => {
            if (e.key === "Enter" && !e.shiftKey) {
              e.preventDefault()
              handleSend()
            }
          }}
          placeholder="Type a message…"
          className="h-8 flex-1 border-0 bg-transparent px-0 text-sm shadow-none focus-visible:ring-0"
        />
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
