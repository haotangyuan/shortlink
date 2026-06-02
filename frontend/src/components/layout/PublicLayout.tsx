import { Link, Outlet } from "react-router-dom";

export function PublicLayout() {
  return (
    <main className="min-h-screen bg-slate-50">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-4">
          <Link to="/" className="text-lg font-semibold text-slate-950">
            ShortLink
          </Link>
          <nav className="flex items-center gap-2 text-sm">
            <Link className="rounded-md px-3 py-2 text-slate-600 hover:bg-slate-100" to="/login">
              登录
            </Link>
            <Link className="rounded-md bg-blue-600 px-3 py-2 text-white" to="/register">
              注册
            </Link>
          </nav>
        </div>
      </header>
      <Outlet />
      <footer className="border-t border-slate-200 bg-white px-4 py-6 text-center text-sm text-slate-500">
        ShortLink 短链接管理平台
      </footer>
    </main>
  );
}
