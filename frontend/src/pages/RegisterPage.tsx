import { useState, type FormEvent } from "react";
import { useAuth } from "../auth/AuthContext";

export function RegisterPage({ onSwitchToLogin }: { onSwitchToLogin: () => void }) {
  const { register } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [fullName, setFullName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    const errorMessage = await register(email, password, fullName);
    setSubmitting(false);
    if (errorMessage) setError(errorMessage);
  }

  return (
    <form onSubmit={handleSubmit}>
      <h2>Register</h2>
      <label>
        Full name
        <input value={fullName} onChange={(e) => setFullName(e.target.value)} required />
      </label>
      <label>
        Email
        <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
      </label>
      <label>
        Password
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          minLength={10}
          required
        />
      </label>
      {error && <p role="alert">{error}</p>}
      <button type="submit" disabled={submitting}>
        {submitting ? "Registering..." : "Register"}
      </button>
      <p>
        Already have an account? <button type="button" onClick={onSwitchToLogin}>Log in</button>
      </p>
    </form>
  );
}
