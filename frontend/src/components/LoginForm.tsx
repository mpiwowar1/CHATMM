import { useEffect, useState } from "react"
import { useLogin } from "../hooks/useLogin"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import {
  Card,
  CardAction,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

interface LoginFormProps {
  onSwitchToRegister: () => void
  onLoggingIn?: () => void
}

type Fields = {
  email: string
  password: string
}

export default function LoginForm({
  onSwitchToRegister,
  onLoggingIn,
}: LoginFormProps) {
  const [fields, setFields] = useState<Fields>({ email: "", password: "" })
  const [rememberMe, setRememberMe] = useState(false)

  const { login, status, error, authData } = useLogin()

  useEffect(() => {
    if (status === "success") {
      onLoggingIn?.()
    }
  }, [status, onLoggingIn])

  const onChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target
    setFields((f) => ({ ...f, [name]: value }))
  }

  const onSubmit = async () => {
    if (fields.email && fields.password) {
      await login({ ...fields, rememberMe })
    }
  }

  const loading = status === "loading"

  if (status === "success" && authData) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Card className="w-full max-w-sm">
          <CardHeader>
            <CardTitle>Welcome back!</CardTitle>
            <CardDescription>
              You are logged in as {authData.name}
            </CardDescription>
          </CardHeader>
          <CardContent>
            <p className="text-sm font-medium text-green-600">
              Login successful. Redirecting...
            </p>
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div className="flex min-h-screen items-center justify-center">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle>Login to your account</CardTitle>
          <CardDescription>
            Enter your email below to login to your account
          </CardDescription>
          <CardAction>
            <Button variant="link" onClick={onSwitchToRegister}>
              Sign Up
            </Button>
          </CardAction>
        </CardHeader>
        <CardContent>
          <div className="flex flex-col gap-6">
            <div className="grid gap-2">
              <Label htmlFor="email">Email</Label>
              <Input
                name="email"
                id="email"
                type="email"
                placeholder="m@example.com"
                onChange={onChange}
                value={fields.email}
                required
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="password">Password</Label>
              <Input
                id="password"
                name="password"
                type="password"
                onChange={onChange}
                value={fields.password}
                required
              />
            </div>
            <div className="flex items-center gap-2">
              <Checkbox
                id="rememberMe"
                checked={rememberMe}
                onCheckedChange={(checked) => setRememberMe(checked === true)}
              />
              <Label
                htmlFor="rememberMe"
                className="cursor-pointer text-sm font-normal select-none"
              >
                Remember me on this device
              </Label>
            </div>
          </div>
        </CardContent>
        <CardFooter className="flex-col gap-2">
          <Button
            type="submit"
            onClick={onSubmit}
            className="w-full"
            disabled={loading}
          >
            {loading ? "Deriving keys…" : "Login"}
          </Button>
          {error && (
            <p role="alert" className="text-sm font-medium text-red-600">
              {error}
            </p>
          )}
        </CardFooter>
      </Card>
    </div>
  )
}
