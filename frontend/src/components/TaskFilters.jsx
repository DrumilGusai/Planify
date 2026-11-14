export function TaskFilters({
  searchText,
  onSearchChange,
  filters,
  onFilterChange,
  members = [],
}) {
  return (
    <div className="filter-bar">
      <input
        className="input"
        type="search"
        placeholder="Search title or description..."
        value={searchText}
        onChange={(e) => onSearchChange(e.target.value)}
      />

      <select
        className="select"
        aria-label="Status filter"
        value={filters.status || ""}
        onChange={(e) => onFilterChange("status", e.target.value)}
      >
        <option value="">All Statuses</option>
        <option value="TODO">TODO</option>
        <option value="IN_PROGRESS">IN_PROGRESS</option>
        <option value="IN_REVIEW">IN_REVIEW</option>
        <option value="DONE">DONE</option>
      </select>

      <select
        className="select"
        aria-label="Priority filter"
        value={filters.priority || ""}
        onChange={(e) => onFilterChange("priority", e.target.value)}
      >
        <option value="">All Priorities</option>
        <option value="LOW">LOW</option>
        <option value="MEDIUM">MEDIUM</option>
        <option value="HIGH">HIGH</option>
        <option value="URGENT">URGENT</option>
      </select>

      <select
        className="select"
        aria-label="Assignee filter"
        value={filters.assigneeId || ""}
        onChange={(e) => onFilterChange("assigneeId", e.target.value)}
      >
        <option value="">All Assignees</option>
        {members.map((m) => (
          <option key={m.userId} value={m.userId}>
            {m.name}
          </option>
        ))}
      </select>

      <input
        className="input"
        type="date"
        aria-label="Due from"
        title="Due from"
        value={filters.dueFrom || ""}
        onChange={(e) => onFilterChange("dueFrom", e.target.value)}
      />

      <select
        className="select"
        aria-label="Sort by"
        value={filters.sort || "createdAt,desc"}
        onChange={(e) => onFilterChange("sort", e.target.value)}
      >
        <option value="createdAt,desc">Newest first</option>
        <option value="dueDate,asc">Due date (asc)</option>
        <option value="dueDate,desc">Due date (desc)</option>
        <option value="priority,desc">Priority</option>
        <option value="title,asc">Title (A-Z)</option>
      </select>
    </div>
  );
}
