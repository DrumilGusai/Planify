import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { workspacesApi } from "../api/workspaces";
import { projectsApi } from "../api/projects";
import { MemberList } from "../components/MemberList";
import { useToast } from "../components/ToastContext";

export function WorkspaceDashboard() {
  const { wsId } = useParams();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const { show, showError } = useToast();

  const [tab, setTab] = useState("projects");
  const [projectModal, setProjectModal] = useState(false);
  const [projName, setProjName] = useState("");
  const [projDesc, setProjDesc] = useState("");

  const [memberEmail, setMemberEmail] = useState("");
  const [memberRole, setMemberRole] = useState("MEMBER");

  const { data: workspace, isLoading: wsLoading } = useQuery({
    queryKey: ["workspace", wsId],
    queryFn: () => workspacesApi.get(wsId),
  });

  const { data: projects = [] } = useQuery({
    queryKey: ["projects", wsId],
    queryFn: () => projectsApi.listByWorkspace(wsId),
  });

  const { data: members = [] } = useQuery({
    queryKey: ["workspace-members", wsId],
    queryFn: () => workspacesApi.listMembers(wsId),
  });

  const canManage = workspace?.myRole === "OWNER" || workspace?.myRole === "ADMIN";
  const isOwner = workspace?.myRole === "OWNER";

  const createProjectMut = useMutation({
    mutationFn: (data) => projectsApi.create(wsId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["projects", wsId] });
      show("Project created", "success");
      setProjName("");
      setProjDesc("");
      setProjectModal(false);
    },
    onError: (err) => showError(err, "Failed to create project"),
  });

  const addMemberMut = useMutation({
    mutationFn: (data) => workspacesApi.addMember(wsId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["workspace-members", wsId] });
      show("Member added", "success");
      setMemberEmail("");
    },
    onError: (err) => showError(err, "Failed to add member"),
  });

  const roleChangeMut = useMutation({
    mutationFn: ({ userId, role }) =>
      workspacesApi.updateMemberRole(wsId, userId, role),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["workspace-members", wsId] });
      show("Role updated", "success");
    },
    onError: (err) => showError(err, "Failed to update role"),
  });

  const removeMemberMut = useMutation({
    mutationFn: (userId) => workspacesApi.removeMember(wsId, userId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["workspace-members", wsId] });
      show("Member removed", "success");
    },
    onError: (err) => showError(err, "Failed to remove member"),
  });

  const deleteWsMut = useMutation({
    mutationFn: () => workspacesApi.remove(wsId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["workspaces"] });
      show("Workspace deleted", "success");
      navigate("/workspaces");
    },
    onError: (err) => showError(err, "Failed to delete workspace"),
  });

  if (wsLoading) return <div className="spinner">Loading workspace...</div>;

  return (
    <div>
      <div className="breadcrumbs">
        <Link to="/workspaces">Workspaces</Link>
        <span>/</span>
        <span>{workspace?.name}</span>
      </div>

      <div className="page-header">
        <div>
          <h1 className="page-title">{workspace?.name}</h1>
          <p className="page-subtitle">{workspace?.description || "Workspace overview"}</p>
        </div>
        <div style={{ display: "flex", gap: 8 }}>
          {canManage && (
            <Link to={`/workspaces/${wsId}/audit`} className="btn btn-secondary">
              Audit Log
            </Link>
          )}
          {isOwner && (
            <button
              className="btn btn-danger"
              onClick={() => {
                if (window.confirm("Delete this workspace and all its projects/tasks?")) {
                  deleteWsMut.mutate();
                }
              }}
            >
              Delete Workspace
            </button>
          )}
          <button
            className="btn btn-primary"
            onClick={() => setProjectModal(true)}
          >
            + New Project
          </button>
        </div>
      </div>

      <div className="tabs">
        <button
          className={`tab-btn ${tab === "projects" ? "active" : ""}`}
          onClick={() => setTab("projects")}
        >
          Projects ({projects.length})
        </button>
        <button
          className={`tab-btn ${tab === "members" ? "active" : ""}`}
          onClick={() => setTab("members")}
        >
          Members ({members.length})
        </button>
      </div>

      {tab === "projects" ? (
        projects.length === 0 ? (
          <div className="card empty-state">No projects visible yet.</div>
        ) : (
          <div className="grid-cards">
            {projects.map((p) => (
              <Link
                key={p.id}
                to={`/projects/${p.id}`}
                className="card"
                style={{ display: "block", color: "inherit", textDecoration: "none" }}
              >
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 8 }}>
                  <strong>{p.name}</strong>
                  <span className={`badge ${p.status === "ARCHIVED" ? "badge-warning" : "badge-success"}`}>
                    {p.status}
                  </span>
                </div>
                <p style={{ margin: "0 0 10px", color: "var(--text-muted)", fontSize: "0.88rem" }}>
                  {p.description || "No description"}
                </p>
                <span className="badge badge-neutral">{p.myRole}</span>
              </Link>
            ))}
          </div>
        )
      ) : (
        <div className="card">
          {canManage && (
            <form
              onSubmit={(e) => {
                e.preventDefault();
                addMemberMut.mutate({ email: memberEmail, role: memberRole });
              }}
              style={{ display: "flex", gap: 10, marginBottom: 18, flexWrap: "wrap" }}
            >
              <input
                className="input"
                type="email"
                required
                placeholder="Existing user email..."
                style={{ flex: 2, minWidth: 220 }}
                value={memberEmail}
                onChange={(e) => setMemberEmail(e.target.value)}
              />
              <select
                className="select"
                style={{ flex: 1, minWidth: 130 }}
                value={memberRole}
                onChange={(e) => setMemberRole(e.target.value)}
              >
                <option value="MEMBER">MEMBER</option>
                <option value="ADMIN">ADMIN</option>
              </select>
              <button className="btn btn-primary" disabled={addMemberMut.isPending}>
                Add Member
              </button>
            </form>
          )}
          <MemberList
            members={members}
            roles={["MEMBER", "ADMIN"]}
            canManage={canManage}
            lockedRole="OWNER"
            onRoleChange={(userId, role) => roleChangeMut.mutate({ userId, role })}
            onRemove={(userId) => removeMemberMut.mutate(userId)}
          />
        </div>
      )}

      {projectModal && (
        <div className="modal-backdrop" onClick={() => setProjectModal(false)}>
          <div className="modal-panel" onClick={(e) => e.stopPropagation()}>
            <h2 style={{ marginTop: 0 }}>Create Project</h2>
            <form
              onSubmit={(e) => {
                e.preventDefault();
                createProjectMut.mutate({ name: projName, description: projDesc });
              }}
            >
              <div className="form-group">
                <label className="form-label">Project Name</label>
                <input
                  className="input"
                  required
                  maxLength={100}
                  value={projName}
                  onChange={(e) => setProjName(e.target.value)}
                />
              </div>
              <div className="form-group">
                <label className="form-label">Description</label>
                <textarea
                  className="textarea"
                  rows={3}
                  maxLength={1000}
                  value={projDesc}
                  onChange={(e) => setProjDesc(e.target.value)}
                />
              </div>
              <div style={{ display: "flex", justifyContent: "flex-end", gap: 8 }}>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setProjectModal(false)}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={createProjectMut.isPending}
                >
                  Create
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
