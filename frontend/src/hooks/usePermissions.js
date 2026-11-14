import { useQuery } from "@tanstack/react-query";
import { projectsApi } from "../api/projects";
import { useAuth } from "../auth/AuthContext";

export function usePermissions(projectId) {
  const { user } = useAuth();

  const { data: project, isLoading } = useQuery({
    queryKey: ["project", String(projectId)],
    queryFn: () => projectsApi.get(projectId),
    enabled: Boolean(projectId),
  });

  const role = project?.myRole || null;
  const isArchived = project?.status === "ARCHIVED";
  const isManager = role === "PROJECT_MANAGER";
  const isContributor = role === "CONTRIBUTOR";

  return {
    project,
    isLoading,
    role,
    isArchived,
    canManage: isManager,
    canEdit: !isArchived && (isManager || isContributor),
    canDeleteTask: (task) =>
      !isArchived &&
      (isManager || (isContributor && task?.createdBy === user?.id)),
  };
}
