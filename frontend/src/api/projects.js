import { api } from "./client";

export const projectsApi = {
  listByWorkspace: (wsId) =>
    api.get(`/workspaces/${wsId}/projects`).then((r) => r.data),
  create: (wsId, data) =>
    api.post(`/workspaces/${wsId}/projects`, data).then((r) => r.data),
  get: (id) => api.get(`/projects/${id}`).then((r) => r.data),
  update: (id, data) => api.put(`/projects/${id}`, data).then((r) => r.data),
  remove: (id) => api.delete(`/projects/${id}`),
  listMembers: (id) => api.get(`/projects/${id}/members`).then((r) => r.data),
  addMember: (id, data) =>
    api.post(`/projects/${id}/members`, data).then((r) => r.data),
  updateMemberRole: (id, userId, role) =>
    api.patch(`/projects/${id}/members/${userId}`, { role }).then((r) => r.data),
  removeMember: (id, userId) =>
    api.delete(`/projects/${id}/members/${userId}`),
};
