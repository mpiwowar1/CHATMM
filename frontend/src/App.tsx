import { useState } from "react"
import LoginForm from "./components/LoginForm"
import RegisterForm from "./components/RegisterForm"

export function App() {
  const [view, setView] = useState<"login" | "register">("login")

  return (
    <>
      {view === "login" ? (
        <LoginForm onSwitchToRegister={() => setView("register")} />
      ) : (
        <RegisterForm onSwitchToLogin={() => setView("login")} />
      )}
    </>
  )
}

export default App
