import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip"
import { LogOut, Settings, Sun, Moon } from "lucide-react"
import { useTheme } from "@/components/theme-provider"
import type { UserResponse } from "./chat-types"

/** User block shown in the sidebar with theme and logout actions. */
function SidebarUserBlock({
  user,
  onLogout,
}: {
  user: UserResponse
  onLogout?: () => void
}) {
  const { theme, setTheme } = useTheme()

  const toggleTheme = () => {
    setTheme(theme === "dark" ? "light" : "dark")
  }
  return (
    <div className="group flex items-center gap-3 rounded-lg px-3 py-2.5 transition-colors hover:bg-accent">
      <Avatar className="h-8 w-8 shrink-0">
        <AvatarFallback className="text-xs font-semibold">
          {user.name.slice(0, 2).toUpperCase()}
        </AvatarFallback>
      </Avatar>

      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-medium">{user.name}</p>
        <p className="truncate text-xs text-muted-foreground">{user.email}</p>
      </div>

      <DropdownMenu>
        <TooltipProvider delayDuration={300}>
          <Tooltip>
            <TooltipTrigger asChild>
              <DropdownMenuTrigger asChild>
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-7 w-7 shrink-0 opacity-0 transition-opacity group-hover:opacity-100"
                >
                  <Settings className="h-3.5 w-3.5" />
                </Button>
              </DropdownMenuTrigger>
            </TooltipTrigger>
            <TooltipContent side="right">Settings</TooltipContent>
          </Tooltip>
        </TooltipProvider>

        <DropdownMenuContent side="top" align="end" className="w-48">
          <DropdownMenuLabel>Appearance</DropdownMenuLabel>
          <DropdownMenuItem className="cursor-pointer">
            {theme === "dark" ? (
              <Moon className="mr-2 h-4 w-4" />
            ) : (
              <Sun className="mr-2 h-4 w-4" />
            )}
            <span className="mr-2">{theme === "dark" ? "Dark" : "Light"}</span>
            <Button
              variant="ghost"
              size="sm"
              className="ml-auto h-7"
              onClick={(e) => {
                e.stopPropagation()
                toggleTheme()
              }}
            >
              Toggle
            </Button>
          </DropdownMenuItem>
          <DropdownMenuSeparator />
          <DropdownMenuLabel>Settings</DropdownMenuLabel>
          <DropdownMenuSeparator />
          <DropdownMenuItem
            onClick={onLogout}
            className="cursor-pointer text-destructive focus:text-destructive"
          >
            <LogOut className="mr-2 h-4 w-4" />
            Log out
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  )
}

export default SidebarUserBlock
