import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { AppProviders } from "./providers";

describe("AppRouter", () => {
  it("renders a lazily loaded page", async () => {
    window.history.replaceState({}, "", "/app/login");
    const { AppRouter } = await import("./router");

    render(
      <AppProviders>
        <AppRouter />
      </AppProviders>,
    );

    expect(await screen.findByRole("heading", { name: "登录控制台" })).toBeInTheDocument();
  });
});
