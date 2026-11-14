import { describe, it, expect, beforeEach } from "vitest";
import { getToken, setToken } from "../api/client";

describe("API client token store", () => {
  beforeEach(() => {
    setToken(null);
  });

  it("stores and clears access token in memory", () => {
    expect(getToken()).toBeNull();
    setToken("jwt-123");
    expect(getToken()).toBe("jwt-123");
    setToken(null);
    expect(getToken()).toBeNull();
  });
});
