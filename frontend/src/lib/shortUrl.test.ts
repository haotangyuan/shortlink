import { describe, expect, it } from "vitest";
import { toAbsoluteShortUrl } from "./shortUrl";

describe("toAbsoluteShortUrl", () => {
  it("adds a protocol to the bare URL returned by list APIs", () => {
    expect(toAbsoluteShortUrl("127.0.0.1:8068/eVQVu9")).toBe("http://127.0.0.1:8068/eVQVu9");
  });

  it("preserves existing HTTP protocols", () => {
    expect(toAbsoluteShortUrl("http://example.com/a")).toBe("http://example.com/a");
    expect(toAbsoluteShortUrl("https://example.com/a")).toBe("https://example.com/a");
  });
});
