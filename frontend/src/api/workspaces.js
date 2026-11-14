import { api } from "./client";

export const workspacesApi = {
  list: () => api.get("/workspaces").then((r) => r.data),
  get: (id) => api.get(`/workspaces/${id}`).then((r) => r.data),
  create: (data) => api.post("/workspaces", data).then((r) => r.data),
  update: (id, data) => api.put(`/workspaces/${id}`, data).then((r) => r.data),
  remove: (id) => api.delete(`/workspaces/${id}`),
  transfer: (id, newOwnerId) =>
    api.post(`/workspaces/${id}/transfer`, { newOwnerId }).then((r) => r.data),
  listMembers: (id) => api.get(`/workspaces/${id}/members`).then((r) => r.data),
  addMember: (id, data) =>
    api.post(`/workspaces/${id}/members`, data).then((r) => r.data),
  updateMemberRole: (id, userId, role) =>
    api.patch(`/workspaces/${id}/members/${userId}`, { role }).then((r) => r.data),
  removeMember: (id, userId) =>
    api.delete(`/workspaces/${id}/members/${userId}`),
};
