import { createContext, useContext, useState, type ReactNode } from "react";
import { apiFetch, setAccessToken } from "../api/client";

interface AuthUser {
  email: string;
  roles: string[];
}

interface AuthContextValue {
  user: AuthUser | null;
  register: (
    email: string,
    password: string,
    fullName: string,
    wantsOwner: boolean,
  ) => Promise<string | null>;
  login: (email: string, password: string) => Promise<string | null>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

async function parseErrorMessage(response: Response): Promise<string> {
  try {
    const body = await response.json();
    return body.message ?? "Request failed";
  } catch {
    return "Request failed";
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);

  async function handleAuthResponse(response: Response): Promise<string | null> {
    if (!response.ok) {
      return parseErrorMessage(response);
    }
    const data = await response.json();
    setAccessToken(data.accessToken);
    setUser({ email: data.email, roles: data.roles });
    return null;
  }

  async function register(email: string, password: string, fullName: string, wantsOwner: boolean) {
    const response = await apiFetch("/auth/register", {
      method: "POST",
      body: JSON.stringify({ email, password, fullName, roles: wantsOwner ? ["OWNER"] : [] }),
    });
    return handleAuthResponse(response);
  }

  async function login(email: string, password: string) {
    const response = await apiFetch("/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    });
    return handleAuthResponse(response);
  }

  async function logout() {
    await apiFetch("/auth/logout", { method: "POST" });
    setAccessToken(null);
    setUser(null);
  }

  return (
    <AuthContext.Provider value={{ user, register, login, logout }}>{children}</AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
