import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { projectsApi } from "../api/projects";
import { workspacesApi } from "../api/workspaces";
import { usePermissions } from "../hooks/usePermissions";
import { MemberList } from "../components/MemberList";
import { useToast } from "../components/ToastContext";

export function Members() {
  const { pid } = useParams();
  const qc = useQueryClient();
  const { show, showError } = useToast();
  const { project, canManage } = usePermissions(pid);

  const [selectedUserId, setSelectedUserId] = useState("");
  const [role, setRole] = useState("CONTRIBUTOR");

  const { data: members = [], isLoading } = useQuery({
    queryKey: ["project-members", String(pid)],
    queryFn: () => projectsApi.listMembers(pid),
  });

  const { data: wsMembers = [] } = useQuery({
    queryKey: ["workspace-members", String(project?.workspaceId)],
    queryFn: () => workspacesApi.listMembers(project.workspaceId),
    enabled: Boolean(project?.workspaceId && canManage),
  });

  const existingIds = new Set(members.map((m) => m.userId));
  const availableWsMembers = wsMembers.filter((m) => !existingIds.has(m.userId));

  const addMutation = useMutation({
    mutationFn: (payload) => projectsApi.addMember(pid, payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["project-members", String(pid)] });
      show("Project member added", "success");
      setSelectedUserId("");
    },
    onError: (err) => showError(err, "Failed to add project member"),
  });

  const roleMutation = useMutation({
    mutationFn: ({ userId, nextRole }) =>
      projectsApi.updateMemberRole(pid, userId, nextRole),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["project-members", String(pid)] });
      show("Member role updated", "success");
    },
    onError: (err) => showError(err, "Failed to update role"),
  });

  const removeMutation = useMutation({
    mutationFn: (userId) => projectsApi.removeMember(pid, userId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["project-members", String(pid)] });
      show("Member removed", "success");
    },
    onError: (err) => showError(err, "Failed to remove member"),
  });

  return (
    <div>
      <div className="breadcrumbs">
        <Link to="/workspaces">Workspaces</Link>
        <span>/</span>
        <Link to={`/projects/${pid}`}>{project?.name || "Project"}</Link>
        <span>/</span>
        <span>Members</span>
      </div>

      <div className="page-header">
        <div>
          <h1 className="page-title">Project Members</h1>
          <p className="page-subtitle">Manage access and roles for {project?.name}</p>
        </div>
        <Link to={`/projects/${pid}`} className="btn btn-secondary">
          Back to Project
        </Link>
      </div>

      <div className="card">
        {canManage && (
          <form
            onSubmit={(e) => {
              e.preventDefault();
              if (!selectedUserId) return;
              addMutation.mutate({ userId: Number(selectedUserId), role });
            }}
            style={{ display: "flex", gap: 10, marginBottom: 18, flexWrap: "wrap" }}
          >
            <select
              className="select"
              style={{ flex: 2, minWidth: 220 }}
              required
              value={selectedUserId}
              onChange={(e) => setSelectedUserId(e.target.value)}
            >
              <option value="">Select workspace member to add...</option>
              {availableWsMembers.map((m) => (
                <option key={m.userId} value={m.userId}>
                  {m.name} ({m.email})
                </option>
              ))}
            </select>
            <select
              className="select"
              style={{ flex: 1, minWidth: 160 }}
              value={role}
              onChange={(e) => setRole(e.target.value)}
            >
              <option value="PROJECT_MANAGER">PROJECT_MANAGER</option>
              <option value="CONTRIBUTOR">CONTRIBUTOR</option>
              <option value="VIEWER">VIEWER</option>
            </select>
            <button className="btn btn-primary" disabled={addMutation.isPending}>
              Add to Project
            </button>
          </form>
        )}

        {isLoading ? (
          <div className="spinner">Loading members...</div>
        ) : (
          <MemberList
            members={members}
            roles={["PROJECT_MANAGER", "CONTRIBUTOR", "VIEWER"]}
            canManage={canManage}
            lockedRole={null}
            onRoleChange={(userId, nextRole) =>
              roleMutation.mutate({ userId, nextRole })
            }
            onRemove={(userId) => removeMutation.mutate(userId)}
          />
        )}
      </div>
    </div>
  );
}
