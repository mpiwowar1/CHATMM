import { useState } from "react"
import { useRegister } from "../hooks/useRegister"
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

type Fields = {
  name: string
  email: string
  password: string
}

interface RegisterFormProps {
  onSwitchToLogin: () => void
}

/** Registration form: generate keys and create a new account. */
export default function RegisterForm({ onSwitchToLogin }: RegisterFormProps) {
  const [fields, setFields] = useState<Fields>({
    name: "",
    email: "",
    password: "",
  })

  const { register, status, error } = useRegister()

  const onChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target

    setFields((f) => ({
      ...f,
      [name]: value,
    }))
  }

  const onSubmit = async () => {
    if (fields.name && fields.email && fields.password) {
      await register(fields)
    }
    return
  }

  const loading = status === "loading"

  return (
    <div className="flex min-h-screen items-center justify-center">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle>Register an account</CardTitle>
          <CardDescription>
            Enter your Username, Email and password to register.
          </CardDescription>
          <CardAction>
            <Button variant="link" onClick={onSwitchToLogin}>
              Log in
            </Button>
          </CardAction>
        </CardHeader>
        <CardContent>
          <form>
            <div className="flex flex-col gap-6">
              <div className="grid gap-2">
                <Label htmlFor="email">Username</Label>
                <Input
                  name="name"
                  type="name"
                  placeholder="Username"
                  onChange={onChange}
                  value={fields.name}
                  required
                />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="email">Email</Label>
                <Input
                  name="email"
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
                </div>
                <Input
                  name="password"
                  onChange={onChange}
                  type="password"
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
            {loading ? "Generating keys…" : "Register"}
          </Button>
          {status === "success" && (
            <p className="text-sm font-medium text-green-600">
              Account created!
            </p>
          )}
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
