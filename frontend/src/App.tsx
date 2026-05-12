import { useState } from "react"
import LoginForm from "./components/LoginForm"
import RegisterForm from "./components/RegisterForm"
import MainScreen from "./components/MainScreen"
import ChatLayout from "./components/chat-layout"

export function App() {
  const [view, setView] = useState<"login" | "register" | "main">("login")

  return (
    <>
      {view === "login" ? (
        <LoginForm
          onLoggingIn={() => setView("main")}
          onSwitchToRegister={() => setView("register")}
        />
      ) : view === "register" ? (
        <RegisterForm onSwitchToLogin={() => setView("login")} />
      ) : (
        <MainScreen></MainScreen>
      )}
    </>
  )
}

export default App
