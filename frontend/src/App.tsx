import { useState } from "react";
import { AuthProvider, useAuth } from "./auth/AuthContext";
import { LoginPage } from "./pages/LoginPage";
import { RegisterPage } from "./pages/RegisterPage";
import { DashboardPage } from "./pages/DashboardPage";

function AppContent() {
  const { user } = useAuth();
  const [view, setView] = useState<"login" | "register">("login");

  if (user) {
    return <DashboardPage />;
  }

  return view === "login" ? (
    <LoginPage onSwitchToRegister={() => setView("register")} />
  ) : (
    <RegisterPage onSwitchToLogin={() => setView("login")} />
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
