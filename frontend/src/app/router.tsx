import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { DashboardLayout } from "../components/layout/DashboardLayout";
import { ProtectedRoute } from "../components/layout/ProtectedRoute";
import { PublicLayout } from "../components/layout/PublicLayout";
import { LoginPage } from "../features/auth/LoginPage";
import { RegisterPage } from "../features/auth/RegisterPage";
import { AnalyticsPage } from "../features/analytics/AnalyticsPage";
import { DashboardPage } from "../features/dashboard/DashboardPage";
import { TokensPage } from "../features/developer/TokensPage";
import { GroupsPage } from "../features/groups/GroupsPage";
import { LinkCreatePage } from "../features/links/LinkCreatePage";
import { LinkEditPage } from "../features/links/LinkEditPage";
import { LinksPage } from "../features/links/LinksPage";
import { ProfilePage } from "../features/profile/ProfilePage";
import { PublicHomePage } from "../features/public/PublicHomePage";
import { RecycleBinPage } from "../features/recycle/RecycleBinPage";

const router = createBrowserRouter(
  [
    {
      element: <PublicLayout />,
      children: [
        { index: true, element: <PublicHomePage /> },
        { path: "login", element: <LoginPage /> },
        { path: "register", element: <RegisterPage /> },
      ],
    },
    {
      path: "dashboard",
      element: (
        <ProtectedRoute>
          <DashboardLayout />
        </ProtectedRoute>
      ),
      children: [
        { index: true, element: <DashboardPage /> },
        { path: "groups", element: <GroupsPage /> },
        { path: "links", element: <LinksPage /> },
        { path: "links/create", element: <LinkCreatePage /> },
        { path: "links/edit", element: <LinkEditPage /> },
        { path: "recycle", element: <RecycleBinPage /> },
        { path: "analytics", element: <AnalyticsPage /> },
        { path: "developer/token", element: <TokensPage /> },
        { path: "profile", element: <ProfilePage /> },
      ],
    },
  ],
  { basename: "/app" },
);

export function AppRouter() {
  return <RouterProvider router={router} />;
}
