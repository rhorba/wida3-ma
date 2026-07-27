import { useState } from "react";
import { AuthProvider, useAuth } from "./auth/AuthContext";
import { LoginPage } from "./pages/LoginPage";
import { RegisterPage } from "./pages/RegisterPage";
import { DashboardPage } from "./pages/DashboardPage";
import { SearchPage } from "./listings/SearchPage";

function AppContent() {
  const { user } = useAuth();
  const [view, setView] = useState<"login" | "register" | "search">("search");

  if (user) {
    return <DashboardPage />;
  }

  if (view === "login") {
    return <LoginPage onSwitchToRegister={() => setView("register")} />;
  }
  if (view === "register") {
    return <RegisterPage onSwitchToLogin={() => setView("login")} />;
  }
  return (
    <div>
      <nav>
        <button type="button" onClick={() => setView("login")}>Log in</button>
        <button type="button" onClick={() => setView("register")}>Register</button>
      </nav>
      <SearchPage />
    </div>
  );
}

function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}

export default App;
