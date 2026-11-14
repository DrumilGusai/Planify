import { api } from "./client";

export const auditApi = {
  listForWorkspace: (wsId, params) =>
    api.get(`/workspaces/${wsId}/audit-logs`, { params }).then((r) => r.data),
  listForProject: (projectId, params) =>
    api.get(`/projects/${projectId}/audit-logs`, { params }).then((r) => r.data),
};
