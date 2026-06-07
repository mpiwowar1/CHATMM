import ChatLayout from "./chat-layout"

interface MainScreenProps {
  onLogout?: () => void
}

/** Main application screen — wraps the chat layout. */
export default function MainScreen({ onLogout }: MainScreenProps) {
  return <ChatLayout onLogout={onLogout} />
}
