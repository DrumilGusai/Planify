import { useState } from "react";
import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { workspacesApi } from "../api/workspaces";
import { useToast } from "../components/ToastContext";

export function Workspaces() {
  const qc = useQueryClient();
  const { show, showError } = useToast();
  const [modalOpen, setModalOpen] = useState(false);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");

  const { data: workspaces = [], isLoading } = useQuery({
    queryKey: ["workspaces"],
    queryFn: workspacesApi.list,
  });

  const createMutation = useMutation({
    mutationFn: workspacesApi.create,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["workspaces"] });
      show("Workspace created", "success");
      setName("");
      setDescription("");
      setModalOpen(false);
    },
    onError: (err) => showError(err, "Failed to create workspace"),
  });

  const handleCreate = (e) => {
    e.preventDefault();
    createMutation.mutate({ name, description });
  };

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Workspaces</h1>
          <p className="page-subtitle">
            Select a workspace to manage projects, members, and tasks.
          </p>
        </div>
        <button className="btn btn-primary" onClick={() => setModalOpen(true)}>
          + New Workspace
        </button>
      </div>

      {isLoading ? (
        <div className="spinner">Loading workspaces...</div>
      ) : workspaces.length === 0 ? (
        <div className="card empty-state">
          <p>You are not part of any workspace yet.</p>
          <button
            className="btn btn-primary"
            onClick={() => setModalOpen(true)}
          >
            Create your first workspace
          </button>
        </div>
      ) : (
        <div className="grid-cards">
          {workspaces.map((ws) => (
            <Link
              key={ws.id}
              to={`/workspaces/${ws.id}`}
              className="card"
              style={{ display: "block", color: "inherit", textDecoration: "none" }}
            >
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 8 }}>
                <strong style={{ fontSize: "1.08rem" }}>{ws.name}</strong>
                <span className="badge badge-primary">{ws.myRole}</span>
              </div>
              <p style={{ margin: 0, color: "var(--text-muted)", fontSize: "0.9rem" }}>
                {ws.description || "No description"}
              </p>
            </Link>
          ))}
        </div>
      )}

      {modalOpen && (
        <div className="modal-backdrop" onClick={() => setModalOpen(false)}>
          <div className="modal-panel" onClick={(e) => e.stopPropagation()}>
            <h2 style={{ marginTop: 0 }}>Create Workspace</h2>
            <form onSubmit={handleCreate}>
              <div className="form-group">
                <label className="form-label">Name</label>
                <input
                  className="input"
                  required
                  maxLength={100}
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                />
              </div>
              <div className="form-group">
                <label className="form-label">Description</label>
                <textarea
                  className="textarea"
                  rows={3}
                  maxLength={500}
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                />
              </div>
              <div style={{ display: "flex", justifyContent: "flex-end", gap: 8 }}>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setModalOpen(false)}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={createMutation.isPending}
                >
                  {createMutation.isPending ? "Creating..." : "Create"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
