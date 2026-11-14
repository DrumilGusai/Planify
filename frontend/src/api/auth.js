import { api } from "./client";

export const authApi = {
  register: (data) => api.post("/auth/register", data).then((r) => r.data),
  login: (data) => api.post("/auth/login", data).then((r) => r.data),
  refresh: () => api.post("/auth/refresh").then((r) => r.data),
  logout: () => api.post("/auth/logout"),
  me: () => api.get("/auth/me").then((r) => r.data),
};
