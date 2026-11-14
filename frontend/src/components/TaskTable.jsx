const PRIORITY_CLASS = {
  LOW: "badge-neutral",
  MEDIUM: "badge-primary",
  HIGH: "badge-warning",
  URGENT: "badge-danger",
};

const STATUS_CLASS = {
  TODO: "badge-neutral",
  IN_PROGRESS: "badge-primary",
  IN_REVIEW: "badge-warning",
  DONE: "badge-success",
};

export function TaskTable({
  tasks,
  canEdit,
  onSelect,
  onStatusChange,
}) {
  if (!tasks || tasks.length === 0) {
    return (
      <div className="empty-state">
        No tasks found matching the current filters.
      </div>
    );
  }

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Title</th>
            <th>Status</th>
            <th>Priority</th>
            <th>Assignee</th>
            <th>Due Date</th>
          </tr>
        </thead>
        <tbody>
          {tasks.map((task) => (
            <tr key={task.id}>
              <td>
                <button
                  type="button"
                  onClick={() => onSelect(task.id)}
                  style={{
                    background: "none",
                    border: "none",
                    padding: 0,
                    color: "var(--primary)",
                    fontWeight: 600,
                    cursor: "pointer",
                    textAlign: "left",
                  }}
                >
                  {task.title}
                </button>
              </td>
              <td>
                {canEdit ? (
                  <select
                    className="select"
                    style={{ width: "auto", padding: "4px 8px", fontSize: "0.82rem" }}
                    value={task.status}
                    onChange={(e) => onStatusChange(task.id, e.target.value)}
                  >
                    <option value="TODO">TODO</option>
                    <option value="IN_PROGRESS">IN_PROGRESS</option>
                    <option value="IN_REVIEW">IN_REVIEW</option>
                    <option value="DONE">DONE</option>
                  </select>
                ) : (
                  <span className={`badge ${STATUS_CLASS[task.status] || "badge-neutral"}`}>
                    {task.status}
                  </span>
                )}
              </td>
              <td>
                <span className={`badge ${PRIORITY_CLASS[task.priority] || "badge-neutral"}`}>
                  {task.priority}
                </span>
              </td>
              <td>{task.assigneeName || <span style={{ color: "var(--text-muted)" }}>Unassigned</span>}</td>
              <td>{task.dueDate || "—"}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
