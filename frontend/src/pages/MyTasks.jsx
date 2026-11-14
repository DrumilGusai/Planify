import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { tasksApi } from "../api/tasks";
import { Pagination } from "../components/Pagination";

export function MyTasks() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState("");

  const { data, isLoading } = useQuery({
    queryKey: ["my-tasks", { page, status }],
    queryFn: () =>
      tasksApi.myTasks({
        page,
        size: 20,
        status: status || undefined,
      }),
  });

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">My Tasks</h1>
          <p className="page-subtitle">Tasks assigned to you across accessible projects.</p>
        </div>
        <select
          className="select"
          style={{ width: 180 }}
          value={status}
          onChange={(e) => {
            setStatus(e.target.value);
            setPage(0);
          }}
        >
          <option value="">All Statuses</option>
          <option value="TODO">TODO</option>
          <option value="IN_PROGRESS">IN_PROGRESS</option>
          <option value="IN_REVIEW">IN_REVIEW</option>
          <option value="DONE">DONE</option>
        </select>
      </div>

      <div className="card">
        {isLoading ? (
          <div className="spinner">Loading your tasks...</div>
        ) : !data?.content?.length ? (
          <div className="empty-state">No assigned tasks found.</div>
        ) : (
          <>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Title</th>
                    <th>Status</th>
                    <th>Priority</th>
                    <th>Due Date</th>
                    <th>Project</th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((t) => (
                    <tr key={t.id}>
                      <td style={{ fontWeight: 600 }}>{t.title}</td>
                      <td><span className="badge badge-primary">{t.status}</span></td>
                      <td><span className="badge badge-neutral">{t.priority}</span></td>
                      <td>{t.dueDate || "—"}</td>
                      <td>
                        <button
                          className="btn btn-secondary btn-sm"
                          onClick={() => navigate(`/projects/${t.projectId}?task=${t.id}`)}
                        >
                          Open Project
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination
              page={data.page}
              totalPages={data.totalPages}
              totalElements={data.totalElements}
              onPageChange={setPage}
            />
          </>
        )}
      </div>
    </div>
  );
}
