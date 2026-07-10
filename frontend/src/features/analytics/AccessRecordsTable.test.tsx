import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { AccessRecordsTable } from "./AccessRecordsTable";

describe("AccessRecordsTable", () => {
  it("moves between access record pages", () => {
    const onPageChange = vi.fn();

    render(
      <AccessRecordsTable
        page={{ records: [], total: 25, size: 10, current: 2, pages: 3 }}
        onPageChange={onPageChange}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "上一页" }));
    fireEvent.click(screen.getByRole("button", { name: "下一页" }));

    expect(onPageChange).toHaveBeenNthCalledWith(1, 1);
    expect(onPageChange).toHaveBeenNthCalledWith(2, 3);
  });
});
