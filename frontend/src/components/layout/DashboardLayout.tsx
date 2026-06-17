import {
  BarChart3,
  Boxes,
  Code2,
  Home,
  Link as LinkIcon,
  LogOut,
  Menu,
  Recycle,
  Sparkles,
  UserRound,
  X,
} from "lucide-react";
import { useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { Button } from "../ui/Button";
import { useAuth } from "../../store/auth";
import { cn } from "../../lib/cn";

const navItems = [
  { to: "/dashboard", label: "仪表板", icon: Home },
  { to: "/dashboard/groups", label: "分组管理", icon: Boxes },
  { to: "/dashboard/links", label: "链接管理", icon: LinkIcon },
  { to: "/dashboard/recycle", label: "回收站", icon: Recycle },
  { to: "/dashboard/analytics", label: "数据统计", icon: BarChart3 },
  { to: "/dashboard/ai-chat", label: "AI 运营助手", icon: Sparkles },
  { to: "/dashboard/developer/token", label: "开发者中心", icon: Code2 },
  { to: "/dashboard/profile", label: "个人设置", icon: UserRound },
];

function Sidebar({ onNavigate }: { onNavigate?: () => void }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  async function handleLogout() {
    await logout();
    navigate("/login");
  }

  return (
    <div className="flex h-full flex-col border-r border-slate-200 bg-white">
      <div className="border-b border-slate-100 px-5 py-5">
        <div className="text-lg font-semibold text-slate-950">ShortLink</div>
        <div className="mt-1 truncate text-sm text-slate-500">{user?.realName || user?.username}</div>
      </div>
      <nav className="flex-1 space-y-1 px-3 py-4">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === "/dashboard"}
            onClick={onNavigate}
            className={({ isActive }) =>
              cn(
                "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100",
                isActive && "bg-blue-50 text-blue-700",
              )
            }
          >
            <item.icon className="h-4 w-4" />
            {item.label}
          </NavLink>
        ))}
      </nav>
      <div className="border-t border-slate-100 p-3">
        <Button variant="ghost" className="w-full justify-start" onClick={handleLogout}>
          <LogOut className="h-4 w-4" />
          退出登录
        </Button>
      </div>
    </div>
  );
}

export function DashboardLayout() {
  const [open, setOpen] = useState(false);

  return (
    <div className="min-h-screen bg-slate-50">
      <aside className="fixed inset-y-0 left-0 hidden w-64 lg:block">
        <Sidebar />
      </aside>
      {open ? (
        <div className="fixed inset-0 z-40 lg:hidden">
          <button
            type="button"
            className="absolute inset-0 bg-slate-950/30"
            aria-label="关闭导航"
            onClick={() => setOpen(false)}
          />
          <div className="relative h-full w-72 max-w-[85vw]">
            <Sidebar onNavigate={() => setOpen(false)} />
          </div>
        </div>
      ) : null}
      <div className="lg:pl-64">
        <header className="sticky top-0 z-30 flex h-16 items-center justify-between border-b border-slate-200 bg-white px-4 lg:px-8">
          <Button variant="ghost" className="h-9 w-9 px-0 lg:hidden" onClick={() => setOpen(true)}>
            <Menu className="h-5 w-5" />
          </Button>
          <div className="text-sm font-medium text-slate-600">短链接控制台</div>
          {open ? (
            <Button variant="ghost" className="h-9 w-9 px-0 lg:hidden" onClick={() => setOpen(false)}>
              <X className="h-5 w-5" />
            </Button>
          ) : (
            <div className="h-9 w-9 lg:hidden" />
          )}
        </header>
        <div className="mx-auto max-w-7xl px-4 py-6 lg:px-8">
          <Outlet />
        </div>
      </div>
    </div>
  );
}
