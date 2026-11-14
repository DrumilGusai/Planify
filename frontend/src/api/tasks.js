import { api } from "./client";

export const tasksApi = {
  list: (projectId, params) =>
    api.get(`/projects/${projectId}/tasks`, { params }).then((r) => r.data),
  myTasks: (params) =>
    api.get("/tasks/my", { params }).then((r) => r.data),
  get: (id) => api.get(`/tasks/${id}`).then((r) => r.data),
  create: (projectId, data) =>
    api.post(`/projects/${projectId}/tasks`, data).then((r) => r.data),
  update: (id, data) => api.put(`/tasks/${id}`, data).then((r) => r.data),
  changeStatus: (id, status) =>
    api.patch(`/tasks/${id}/status`, { status }).then((r) => r.data),
  remove: (id) => api.delete(`/tasks/${id}`),
};
