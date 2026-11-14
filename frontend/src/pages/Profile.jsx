import { useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export function Profile() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate("/login");
  };

  return (
    <div style={{ maxWidth: 560 }}>
      <div className="page-header">
        <div>
          <h1 className="page-title">Profile</h1>
          <p className="page-subtitle">Your Planify account details.</p>
        </div>
      </div>

      <div className="card">
        <div className="form-group">
          <label className="form-label">Name</label>
          <div>{user?.name}</div>
        </div>
        <div className="form-group">
          <label className="form-label">Email</label>
          <div>{user?.email}</div>
        </div>
        <div className="form-group">
          <label className="form-label">Platform Role</label>
          <span className="badge badge-primary">{user?.platformRole}</span>
        </div>

        <button className="btn btn-danger" style={{ marginTop: 12 }} onClick={handleLogout}>
          Sign Out
        </button>
      </div>
    </div>
  );
}
