import { useState } from "react"
import { useLogin } from "../hooks/useLogin"
import { Button } from "@/components/ui/button"
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
}

type Fields = {
  email: string
  password: string
}

export default function LoginForm({ onSwitchToRegister }: LoginFormProps) {
  const [fields, setFields] = useState<Fields>({
    email: "",
    password: "",
  })

  const { login, status, error, authData } = useLogin()

  const onChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target

    setFields((f) => ({
      ...f,
      [name]: value,
    }))
  }

  const onSubmit = async () => {
    if (fields.email && fields.password) {
      await login(fields)
    }
  }

  const loading = status === "loading"

  // If login successful, show welcome message
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
          <form>
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
                <div className="flex items-center">
                  <Label htmlFor="password">Password</Label>
                  <a
                    href="#"
                    className="ml-auto inline-block text-sm underline-offset-4 hover:underline"
                  >
                    Forgot your password?
                  </a>
                </div>
                <Input
                  id="password"
                  name="password"
                  type="password"
                  onChange={onChange}
                  value={fields.password}
                  required
                />
              </div>
            </div>
          </form>
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
