import { createContext, useContext, useEffect, useState } from "react";
import { authApi } from "../api/auth";
import { setToken } from "../api/client";

const AuthContext = createContext(null);

export const useAuth = () => useContext(AuthContext);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    authApi
      .refresh()
      .then((res) => {
        setToken(res.accessToken);
        return authApi.me();
      })
      .then((u) => {
        if (active) setUser(u);
      })
      .catch(() => {
        if (active) setUser(null);
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    const handleLogoutEvent = () => {
      setToken(null);
      setUser(null);
    };
    window.addEventListener("auth:logout", handleLogoutEvent);
    return () => {
      active = false;
      window.removeEventListener("auth:logout", handleLogoutEvent);
    };
  }, []);

  const login = async (email, password) => {
    const res = await authApi.login({ email, password });
    setToken(res.accessToken);
    const me = await authApi.me();
    setUser(me);
    return me;
  };

  const register = async (name, email, password) => {
    return authApi.register({ name, email, password });
  };

  const logout = async () => {
    try {
      await authApi.logout();
    } finally {
      setToken(null);
      setUser(null);
    }
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}
