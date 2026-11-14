import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate("/login");
  };

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="topbar-left">
          <Link to="/workspaces" className="brand">
            Planify
          </Link>
          <nav className="nav-links">
            <NavLink
              to="/workspaces"
              className={({ isActive }) =>
                `nav-link ${isActive ? "active" : ""}`
              }
            >
              Workspaces
            </NavLink>
            <NavLink
              to="/tasks/my"
              className={({ isActive }) =>
                `nav-link ${isActive ? "active" : ""}`
              }
            >
              My Tasks
            </NavLink>
            <NavLink
              to="/profile"
              className={({ isActive }) =>
                `nav-link ${isActive ? "active" : ""}`
              }
            >
              Profile
            </NavLink>
          </nav>
        </div>
        <div className="topbar-right">
          <span style={{ fontSize: "0.88rem", color: "var(--text-muted)" }}>
            {user?.name}
          </span>
          <button className="btn btn-secondary btn-sm" onClick={handleLogout}>
            Logout
          </button>
        </div>
      </header>
      <main className="main-content">
        <Outlet />
      </main>
    </div>
  );
}
