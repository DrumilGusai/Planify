import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { useToast } from "../components/ToastContext";

export function Login() {
  const { login } = useAuth();
  const { showError } = useToast();
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await login(email, password);
      navigate("/workspaces");
    } catch (err) {
      showError(err, "Login failed");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="card auth-card">
        <h1 className="page-title" style={{ marginBottom: 6 }}>
          Sign in to Planify
        </h1>
        <p className="page-subtitle" style={{ marginBottom: 20 }}>
          Manage your workspaces, projects, and tasks.
        </p>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Email</label>
            <input
              className="input"
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@company.com"
            />
          </div>

          <div className="form-group">
            <label className="form-label">Password</label>
            <input
              className="input"
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
            />
          </div>

          <button
            className="btn btn-primary"
            style={{ width: "100%", marginTop: 6 }}
            disabled={submitting}
          >
            {submitting ? "Signing in..." : "Sign in"}
          </button>
        </form>

        <p style={{ marginTop: 18, fontSize: "0.9rem", textAlign: "center" }}>
          New to Planify? <Link to="/register">Create an account</Link>
        </p>
      </div>
    </div>
  );
}
