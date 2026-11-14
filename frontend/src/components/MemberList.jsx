export function MemberList({
  members,
  roles,
  canManage,
  lockedRole = "OWNER",
  onRoleChange,
  onRemove,
}) {
  if (!members || members.length === 0) {
    return <div className="empty-state">No members yet.</div>;
  }

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Name</th>
            <th>Email</th>
            <th>Role</th>
            {canManage && <th>Actions</th>}
          </tr>
        </thead>
        <tbody>
          {members.map((m) => {
            const isLocked = m.role === lockedRole;
            return (
              <tr key={m.userId}>
                <td style={{ fontWeight: 500 }}>{m.name}</td>
                <td style={{ color: "var(--text-muted)" }}>{m.email}</td>
                <td>
                  {canManage && !isLocked ? (
                    <select
                      className="select"
                      style={{ width: "auto", padding: "4px 8px", fontSize: "0.84rem" }}
                      value={m.role}
                      onChange={(e) => onRoleChange(m.userId, e.target.value)}
                    >
                      {roles.map((r) => (
                        <option key={r} value={r}>
                          {r}
                        </option>
                      ))}
                    </select>
                  ) : (
                    <span className="badge badge-primary">{m.role}</span>
                  )}
                </td>
                {canManage && (
                  <td>
                    {!isLocked && (
                      <button
                        className="btn btn-danger btn-sm"
                        onClick={() => onRemove(m.userId)}
                      >
                        Remove
                      </button>
                    )}
                  </td>
                )}
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
