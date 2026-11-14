export function Pagination({ page, totalPages, totalElements, onPageChange }) {
  if (!totalPages || totalPages <= 1) {
    return (
      <div className="pagination">
        <span>Total: {totalElements ?? 0}</span>
      </div>
    );
  }

  return (
    <div className="pagination">
      <span>
        Page {page + 1} of {totalPages} ({totalElements} items)
      </span>
      <div style={{ display: "flex", gap: 8 }}>
        <button
          className="btn btn-secondary btn-sm"
          disabled={page <= 0}
          onClick={() => onPageChange(page - 1)}
        >
          Previous
        </button>
        <button
          className="btn btn-secondary btn-sm"
          disabled={page + 1 >= totalPages}
          onClick={() => onPageChange(page + 1)}
        >
          Next
        </button>
      </div>
    </div>
  );
}
