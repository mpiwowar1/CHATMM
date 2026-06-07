import ChatLayout from "./chat-layout"

interface MainScreenProps {
  onLogout?: () => void
}

export default function MainScreen({ onLogout }: MainScreenProps) {
  return <ChatLayout onLogout={onLogout} />
}
