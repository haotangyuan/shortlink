import { useEffect, useState } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../../store/auth";

export function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, checkAuth } = useAuth();
  const location = useLocation();
  const [verified, setVerified] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setVerified(false);
    async function verify() {
      if (isAuthenticated) {
        await checkAuth();
      }
      if (!cancelled) setVerified(true);
    }
    verify();
    return () => { cancelled = true; };
  }, [checkAuth, isAuthenticated]);

  if (!verified) return null;

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  return children;
}
