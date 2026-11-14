import { describe, it, expect, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { TaskFilters } from "../components/TaskFilters";

describe("TaskFilters", () => {
  it("calls handlers when search and status change", () => {
    const onSearchChange = vi.fn();
    const onFilterChange = vi.fn();

    render(
      <TaskFilters
        searchText=""
        onSearchChange={onSearchChange}
        filters={{ status: "", priority: "", assigneeId: "", dueFrom: "", sort: "createdAt,desc" }}
        onFilterChange={onFilterChange}
        members={[{ userId: 1, name: "Alice" }]}
      />
    );

    fireEvent.change(screen.getByPlaceholderText(/search title/i), {
      target: { value: "redis" },
    });
    expect(onSearchChange).toHaveBeenCalledWith("redis");

    fireEvent.change(screen.getByLabelText("Status filter"), {
      target: { value: "DONE" },
    });
    expect(onFilterChange).toHaveBeenCalledWith("status", "DONE");
  });
});
