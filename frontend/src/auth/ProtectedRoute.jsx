import { Navigate } from "react-router-dom";
import { useAuth } from "./AuthContext";

export function ProtectedRoute({ children }) {
  const { user, loading } = useAuth();

  if (loading) {
    return <div className="spinner">Loading session...</div>;
  }
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  return children;
}
