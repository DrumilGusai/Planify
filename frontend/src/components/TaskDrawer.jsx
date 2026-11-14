import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { tasksApi } from "../api/tasks";
import { useToast } from "./ToastContext";

export function TaskDrawer({
  taskId,
  projectId,
  members = [],
  canEdit,
  canDeleteTask,
  onClose,
}) {
  const qc = useQueryClient();
  const { show, showError } = useToast();

  const { data: task, isLoading } = useQuery({
    queryKey: ["task", String(taskId)],
    queryFn: () => tasksApi.get(taskId),
    enabled: Boolean(taskId),
  });

  const [form, setForm] = useState({
    title: "",
    description: "",
    status: "TODO",
    priority: "MEDIUM",
    assigneeId: "",
    dueDate: "",
  });

  useEffect(() => {
    if (task) {
      setForm({
        title: task.title || "",
        description: task.description || "",
        status: task.status || "TODO",
        priority: task.priority || "MEDIUM",
        assigneeId: task.assigneeId ? String(task.assigneeId) : "",
        dueDate: task.dueDate || "",
      });
    }
  }, [task]);

  const updateMutation = useMutation({
    mutationFn: (payload) => tasksApi.update(taskId, payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["tasks", String(projectId)] });
      qc.invalidateQueries({ queryKey: ["task", String(taskId)] });
      show("Task updated", "success");
      onClose();
    },
    onError: (err) => showError(err, "Failed to update task"),
  });

  const deleteMutation = useMutation({
    mutationFn: () => tasksApi.remove(taskId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["tasks", String(projectId)] });
      show("Task deleted", "success");
      onClose();
    },
    onError: (err) => showError(err, "Failed to delete task"),
  });

  if (!taskId) return null;

  const handleSubmit = (e) => {
    e.preventDefault();
    updateMutation.mutate({
      title: form.title,
      description: form.description,
      status: form.status,
      priority: form.priority,
      assigneeId: form.assigneeId ? Number(form.assigneeId) : null,
      dueDate: form.dueDate || null,
    });
  };

  return (
    <div className="drawer-backdrop" onClick={onClose}>
      <div className="drawer-panel" onClick={(e) => e.stopPropagation()}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 18 }}>
          <h2 style={{ margin: 0, fontSize: "1.2rem" }}>Task Details</h2>
          <button className="btn btn-secondary btn-sm" onClick={onClose}>
            Close
          </button>
        </div>

        {isLoading || !task ? (
          <div className="spinner">Loading task...</div>
        ) : (
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label className="form-label">Title</label>
              <input
                className="input"
                required
                disabled={!canEdit}
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
              />
            </div>

            <div className="form-group">
              <label className="form-label">Description</label>
              <textarea
                className="textarea"
                rows={4}
                disabled={!canEdit}
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
              />
            </div>

            <div className="form-group">
              <label className="form-label">Status</label>
              <select
                className="select"
                disabled={!canEdit}
                value={form.status}
                onChange={(e) => setForm({ ...form, status: e.target.value })}
              >
                <option value="TODO">TODO</option>
                <option value="IN_PROGRESS">IN_PROGRESS</option>
                <option value="IN_REVIEW">IN_REVIEW</option>
                <option value="DONE">DONE</option>
              </select>
            </div>

            <div className="form-group">
              <label className="form-label">Priority</label>
              <select
                className="select"
                disabled={!canEdit}
                value={form.priority}
                onChange={(e) => setForm({ ...form, priority: e.target.value })}
              >
                <option value="LOW">LOW</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="HIGH">HIGH</option>
                <option value="URGENT">URGENT</option>
              </select>
            </div>

            <div className="form-group">
              <label className="form-label">Assignee</label>
              <select
                className="select"
                disabled={!canEdit}
                value={form.assigneeId}
                onChange={(e) => setForm({ ...form, assigneeId: e.target.value })}
              >
                <option value="">Unassigned</option>
                {members.map((m) => (
                  <option key={m.userId} value={m.userId}>
                    {m.name} ({m.email})
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label">Due Date</label>
              <input
                className="input"
                type="date"
                disabled={!canEdit}
                value={form.dueDate}
                onChange={(e) => setForm({ ...form, dueDate: e.target.value })}
              />
            </div>

            <div style={{ display: "flex", justifyContent: "space-between", marginTop: 22 }}>
              {canDeleteTask(task) ? (
                <button
                  type="button"
                  className="btn btn-danger"
                  disabled={deleteMutation.isPending}
                  onClick={() => deleteMutation.mutate()}
                >
                  Delete Task
                </button>
              ) : (
                <span />
              )}

              {canEdit && (
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={updateMutation.isPending}
                >
                  {updateMutation.isPending ? "Saving..." : "Save Changes"}
                </button>
              )}
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
