import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AuthProvider } from "./auth/AuthContext";
import { ProtectedRoute } from "./auth/ProtectedRoute";
import { ToastProvider } from "./components/ToastContext";
import { Layout } from "./components/Layout";
import { Login } from "./pages/Login";
import { Register } from "./pages/Register";
import { Workspaces } from "./pages/Workspaces";
import { WorkspaceDashboard } from "./pages/WorkspaceDashboard";
import { ProjectBoard } from "./pages/ProjectBoard";
import { Members } from "./pages/Members";
import { AuditLog } from "./pages/AuditLog";
import { MyTasks } from "./pages/MyTasks";
import { Profile } from "./pages/Profile";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 15_000,
    },
  },
});

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <AuthProvider>
          <BrowserRouter>
            <Routes>
              <Route path="/login" element={<Login />} />
              <Route path="/register" element={<Register />} />

              <Route
                element={
                  <ProtectedRoute>
                    <Layout />
                  </ProtectedRoute>
                }
              >
                <Route path="/" element={<Navigate to="/workspaces" replace />} />
                <Route path="/workspaces" element={<Workspaces />} />
                <Route path="/workspaces/:wsId" element={<WorkspaceDashboard />} />
                <Route path="/workspaces/:wsId/audit" element={<AuditLog scope="workspace" />} />
                <Route path="/projects/:pid" element={<ProjectBoard />} />
                <Route path="/projects/:pid/members" element={<Members />} />
                <Route path="/projects/:pid/audit" element={<AuditLog scope="project" />} />
                <Route path="/tasks/my" element={<MyTasks />} />
                <Route path="/profile" element={<Profile />} />
              </Route>

              <Route path="*" element={<Navigate to="/workspaces" replace />} />
            </Routes>
          </BrowserRouter>
        </AuthProvider>
      </ToastProvider>
    </QueryClientProvider>
  );
}

export default App;
