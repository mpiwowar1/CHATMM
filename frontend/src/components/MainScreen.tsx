import ChatLayout from "./chat-layout"

interface MainScreenProps {
  onLogout?: () => void
  authVersion?: number
}

/** Main application screen — wraps the chat layout. */
export default function MainScreen({ onLogout, authVersion }: MainScreenProps) {
  return <ChatLayout key={authVersion} onLogout={onLogout} />
}
