import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { DashboardLayout } from "../components/layout/DashboardLayout";
import { ProtectedRoute } from "../components/layout/ProtectedRoute";
import { PublicLayout } from "../components/layout/PublicLayout";

const router = createBrowserRouter(
  [
    {
      element: <PublicLayout />,
      children: [
        {
          index: true,
          lazy: async () => ({
            Component: (await import("../features/public/PublicHomePage")).PublicHomePage,
          }),
        },
        {
          path: "login",
          lazy: async () => ({ Component: (await import("../features/auth/LoginPage")).LoginPage }),
        },
        {
          path: "register",
          lazy: async () => ({ Component: (await import("../features/auth/RegisterPage")).RegisterPage }),
        },
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
        {
          index: true,
          lazy: async () => ({
            Component: (await import("../features/dashboard/DashboardPage")).DashboardPage,
          }),
        },
        {
          path: "groups",
          lazy: async () => ({ Component: (await import("../features/groups/GroupsPage")).GroupsPage }),
        },
        {
          path: "links",
          lazy: async () => ({ Component: (await import("../features/links/LinksPage")).LinksPage }),
        },
        {
          path: "links/create",
          lazy: async () => ({
            Component: (await import("../features/links/LinkCreatePage")).LinkCreatePage,
          }),
        },
        {
          path: "links/edit",
          lazy: async () => ({ Component: (await import("../features/links/LinkEditPage")).LinkEditPage }),
        },
        {
          path: "recycle",
          lazy: async () => ({
            Component: (await import("../features/recycle/RecycleBinPage")).RecycleBinPage,
          }),
        },
        {
          path: "analytics",
          lazy: async () => ({
            Component: (await import("../features/analytics/AnalyticsPage")).AnalyticsPage,
          }),
        },
        {
          path: "developer/token",
          lazy: async () => ({
            Component: (await import("../features/developer/TokensPage")).TokensPage,
          }),
        },
        {
          path: "profile",
          lazy: async () => ({ Component: (await import("../features/profile/ProfilePage")).ProfilePage }),
        },
        {
          path: "ai-chat",
          lazy: async () => ({ Component: (await import("../features/ai/AiChatPage")).AiChatPage }),
        },
      ],
    },
  ],
  { basename: "/app" },
);

export function AppRouter() {
  return <RouterProvider router={router} />;
}
