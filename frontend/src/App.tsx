import { useEffect, useState } from "react"
import LoginForm from "./components/LoginForm"
import RegisterForm from "./components/RegisterForm"
import MainScreen from "./components/MainScreen"
import {
  loadWrappedPrivateKey,
  clearDeviceKeyStore,
} from "./encryption/deviceKeyStore"
import { storePrivateKey, clearPrivateKey } from "./encryption/keyStore"

type View = "login" | "register" | "main" | "restoring"

function shouldRestore(): boolean {
  return (
    !!localStorage.getItem("accessToken") &&
    localStorage.getItem("rememberMe") === "true"
  )
}

export function App() {
  const [view, setView] = useState<View>(
    shouldRestore() ? "restoring" : "login"
  )

  useEffect(() => {
    if (view !== "restoring") return

    loadWrappedPrivateKey().then((privateKey) => {
      if (privateKey) {
        storePrivateKey(privateKey)
        setView("main")
      } else {
        setView("login")
      }
    })
  }, [])

  async function handleLogout() {
    clearPrivateKey()
    await clearDeviceKeyStore()
    localStorage.removeItem("accessToken")
    localStorage.removeItem("refreshToken")
    localStorage.removeItem("user")
    localStorage.removeItem("encryptedPrivateKey")
    localStorage.removeItem("deviceId")
    localStorage.removeItem("rememberMe")
    setView("login")
  }

  if (view === "restoring") {
    return (
      <div className="flex min-h-screen items-center justify-center text-sm text-muted-foreground">
        Restoring session…
      </div>
    )
  }

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
        <MainScreen onLogout={handleLogout} />
      )}
    </>
  )
}

export default App
