import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { auditApi } from "../api/audit";
import { Pagination } from "../components/Pagination";

export function AuditLog({ scope }) {
  const { wsId, pid } = useParams();
  const [page, setPage] = useState(0);
  const [entityType, setEntityType] = useState("");

  const params = {
    page,
    size: 20,
    entityType: entityType || undefined,
  };

  const { data, isLoading } = useQuery({
    queryKey: ["audit", scope, wsId || pid, params],
    queryFn: () =>
      scope === "workspace"
        ? auditApi.listForWorkspace(wsId, params)
        : auditApi.listForProject(pid, params),
  });

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">
            {scope === "workspace" ? "Workspace Audit Log" : "Project Audit Log"}
          </h1>
          <p className="page-subtitle">Immutable history of changes and membership events.</p>
        </div>
        <Link
          to={scope === "workspace" ? `/workspaces/${wsId}` : `/projects/${pid}`}
          className="btn btn-secondary"
        >
          Back
        </Link>
      </div>

      <div className="card">
        <div style={{ display: "flex", gap: 10, marginBottom: 16 }}>
          <select
            className="select"
            style={{ maxWidth: 220 }}
            value={entityType}
            onChange={(e) => {
              setEntityType(e.target.value);
              setPage(0);
            }}
          >
            <option value="">All Entity Types</option>
            <option value="WORKSPACE">WORKSPACE</option>
            <option value="PROJECT">PROJECT</option>
            <option value="TASK">TASK</option>
            <option value="MEMBER">MEMBER</option>
          </select>
        </div>

        {isLoading ? (
          <div className="spinner">Loading audit logs...</div>
        ) : !data?.content?.length ? (
          <div className="empty-state">No audit records found.</div>
        ) : (
          <>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Timestamp</th>
                    <th>Actor</th>
                    <th>Entity</th>
                    <th>Action</th>
                    <th>Entity ID</th>
                    <th>IP</th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((row) => (
                    <tr key={row.id}>
                      <td>{new Date(row.createdAt).toLocaleString()}</td>
                      <td>{row.actorName || `User #${row.actorId}`}</td>
                      <td>
                        <span className="badge badge-neutral">{row.entityType}</span>
                      </td>
                      <td>
                        <span className="badge badge-primary">{row.action}</span>
                      </td>
                      <td>{row.entityId ?? "—"}</td>
                      <td style={{ color: "var(--text-muted)" }}>{row.ipAddress || "—"}</td>
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
