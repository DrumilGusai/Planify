import { useState } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";
import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { projectsApi } from "../api/projects";
import { tasksApi } from "../api/tasks";
import { useDebounce } from "../hooks/useDebounce";
import { usePermissions } from "../hooks/usePermissions";
import { TaskFilters } from "../components/TaskFilters";
import { TaskTable } from "../components/TaskTable";
import { TaskDrawer } from "../components/TaskDrawer";
import { Pagination } from "../components/Pagination";
import { useToast } from "../components/ToastContext";

export function ProjectBoard() {
  const { pid } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const qc = useQueryClient();
  const { show, showError } = useToast();

  const { project, role, isArchived, canEdit, canManage, canDeleteTask } =
    usePermissions(pid);

  const [searchText, setSearchText] = useState(searchParams.get("q") || "");
  const debouncedQ = useDebounce(searchText, 300);

  const [createOpen, setCreateOpen] = useState(false);
  const [newTask, setNewTask] = useState({
    title: "",
    description: "",
    priority: "MEDIUM",
    assigneeId: "",
    dueDate: "",
  });

  const queryParams = {
    page: Number(searchParams.get("page")) || 0,
    size: 20,
    sort: searchParams.get("sort") || "createdAt,desc",
    status: searchParams.get("status") || undefined,
    priority: searchParams.get("priority") || undefined,
    assigneeId: searchParams.get("assigneeId") || undefined,
    dueFrom: searchParams.get("dueFrom") || undefined,
    dueTo: searchParams.get("dueTo") || undefined,
    q: debouncedQ || undefined,
  };

  const { data: taskPage, isFetching } = useQuery({
    queryKey: ["tasks", String(pid), queryParams],
    queryFn: () => tasksApi.list(pid, queryParams),
    placeholderData: keepPreviousData,
  });

  const { data: members = [] } = useQuery({
    queryKey: ["project-members", String(pid)],
    queryFn: () => projectsApi.listMembers(pid),
  });

  const createMutation = useMutation({
    mutationFn: (payload) => tasksApi.create(pid, payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["tasks", String(pid)] });
      show("Task created", "success");
      setNewTask({
        title: "",
        description: "",
        priority: "MEDIUM",
        assigneeId: "",
        dueDate: "",
      });
      setCreateOpen(false);
    },
    onError: (err) => showError(err, "Failed to create task"),
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, status }) => tasksApi.changeStatus(id, status),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["tasks", String(pid)] });
      show("Status updated", "success");
    },
    onError: (err) => showError(err, "Failed to update status"),
  });

  const archiveMutation = useMutation({
    mutationFn: (nextStatus) =>
      projectsApi.update(pid, {
        name: project.name,
        description: project.description,
        status: nextStatus,
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["project", String(pid)] });
      show("Project status updated", "success");
    },
    onError: (err) => showError(err, "Failed to update project"),
  });

  const updateParam = (key, value) => {
    const next = new URLSearchParams(searchParams);
    if (value) next.set(key, value);
    else next.delete(key);
    if (key !== "page") next.delete("page");
    setSearchParams(next);
  };

  const selectedTaskId = searchParams.get("task");

  return (
    <div>
      <div className="breadcrumbs">
        <Link to="/workspaces">Workspaces</Link>
        <span>/</span>
        {project?.workspaceId && (
          <>
            <Link to={`/workspaces/${project.workspaceId}`}>Workspace</Link>
            <span>/</span>
          </>
        )}
        <span>{project?.name || "Project"}</span>
      </div>

      <div className="page-header">
        <div>
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <h1 className="page-title">{project?.name || "Project"}</h1>
            {isArchived && <span className="badge badge-warning">ARCHIVED</span>}
            {role && <span className="badge badge-neutral">{role}</span>}
          </div>
          <p className="page-subtitle">{project?.description || "Task board"}</p>
        </div>

        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <Link to={`/projects/${pid}/members`} className="btn btn-secondary">
            Members
          </Link>
          {canManage && (
            <>
              <Link to={`/projects/${pid}/audit`} className="btn btn-secondary">
                Audit Log
              </Link>
              <button
                className="btn btn-secondary"
                onClick={() =>
                  archiveMutation.mutate(isArchived ? "ACTIVE" : "ARCHIVED")
                }
              >
                {isArchived ? "Unarchive" : "Archive"}
              </button>
            </>
          )}
          {canEdit && (
            <button
              className="btn btn-primary"
              onClick={() => setCreateOpen(true)}
            >
              + New Task
            </button>
          )}
        </div>
      </div>

      <div className="card">
        <TaskFilters
          searchText={searchText}
          onSearchChange={(val) => {
            setSearchText(val);
            updateParam("q", val);
          }}
          filters={{
            status: searchParams.get("status") || "",
            priority: searchParams.get("priority") || "",
            assigneeId: searchParams.get("assigneeId") || "",
            dueFrom: searchParams.get("dueFrom") || "",
            sort: searchParams.get("sort") || "createdAt,desc",
          }}
          onFilterChange={updateParam}
          members={members}
        />

        {isFetching && !taskPage ? (
          <div className="spinner">Loading tasks...</div>
        ) : (
          <>
            <TaskTable
              tasks={taskPage?.content || []}
              canEdit={canEdit}
              onSelect={(id) => updateParam("task", String(id))}
              onStatusChange={(id, status) =>
                statusMutation.mutate({ id, status })
              }
            />
            <Pagination
              page={taskPage?.page || 0}
              totalPages={taskPage?.totalPages || 0}
              totalElements={taskPage?.totalElements || 0}
              onPageChange={(nextPage) => updateParam("page", String(nextPage))}
            />
          </>
        )}
      </div>

      <TaskDrawer
        taskId={selectedTaskId}
        projectId={pid}
        members={members}
        canEdit={canEdit}
        canDeleteTask={canDeleteTask}
        onClose={() => updateParam("task", "")}
      />

      {createOpen && (
        <div className="modal-backdrop" onClick={() => setCreateOpen(false)}>
          <div className="modal-panel" onClick={(e) => e.stopPropagation()}>
            <h2 style={{ marginTop: 0 }}>New Task</h2>
            <form
              onSubmit={(e) => {
                e.preventDefault();
                createMutation.mutate({
                  title: newTask.title,
                  description: newTask.description,
                  priority: newTask.priority,
                  assigneeId: newTask.assigneeId ? Number(newTask.assigneeId) : null,
                  dueDate: newTask.dueDate || null,
                });
              }}
            >
              <div className="form-group">
                <label className="form-label">Title</label>
                <input
                  className="input"
                  required
                  maxLength={200}
                  value={newTask.title}
                  onChange={(e) =>
                    setNewTask({ ...newTask, title: e.target.value })
                  }
                />
              </div>
              <div className="form-group">
                <label className="form-label">Description</label>
                <textarea
                  className="textarea"
                  rows={3}
                  value={newTask.description}
                  onChange={(e) =>
                    setNewTask({ ...newTask, description: e.target.value })
                  }
                />
              </div>
              <div className="form-group">
                <label className="form-label">Priority</label>
                <select
                  className="select"
                  value={newTask.priority}
                  onChange={(e) =>
                    setNewTask({ ...newTask, priority: e.target.value })
                  }
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
                  value={newTask.assigneeId}
                  onChange={(e) =>
                    setNewTask({ ...newTask, assigneeId: e.target.value })
                  }
                >
                  <option value="">Unassigned</option>
                  {members.map((m) => (
                    <option key={m.userId} value={m.userId}>
                      {m.name}
                    </option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">Due Date</label>
                <input
                  className="input"
                  type="date"
                  value={newTask.dueDate}
                  onChange={(e) =>
                    setNewTask({ ...newTask, dueDate: e.target.value })
                  }
                />
              </div>
              <div style={{ display: "flex", justifyContent: "flex-end", gap: 8 }}>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setCreateOpen(false)}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={createMutation.isPending}
                >
                  Create Task
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
