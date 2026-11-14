import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { useToast } from "../components/ToastContext";

export function Register() {
  const { register } = useAuth();
  const { show, showError } = useToast();
  const navigate = useNavigate();

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await register(name, email, password);
      show("Account created! Please sign in.", "success");
      navigate("/login");
    } catch (err) {
      showError(err, "Registration failed");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="card auth-card">
        <h1 className="page-title" style={{ marginBottom: 6 }}>
          Create your account
        </h1>
        <p className="page-subtitle" style={{ marginBottom: 20 }}>
          Start organizing your projects in seconds.
        </p>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Full Name</label>
            <input
              className="input"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Alice Smith"
            />
          </div>

          <div className="form-group">
            <label className="form-label">Email</label>
            <input
              className="input"
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="alice@company.com"
            />
          </div>

          <div className="form-group">
            <label className="form-label">Password (min 8 characters)</label>
            <input
              className="input"
              type="password"
              minLength={8}
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
            {submitting ? "Creating account..." : "Create Account"}
          </button>
        </form>

        <p style={{ marginTop: 18, fontSize: "0.9rem", textAlign: "center" }}>
          Already have an account? <Link to="/login">Sign in</Link>
        </p>
      </div>
    </div>
  );
}
