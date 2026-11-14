import axios from "axios";

const base = (import.meta.env.VITE_API_URL || "") + "/api/v1";

export const api = axios.create({
  baseURL: base,
  withCredentials: true,
});

let accessToken = null;
let refreshingPromise = null;

export const setToken = (token) => {
  accessToken = token;
};

export const getToken = () => accessToken;

api.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const config = error.config;
    if (
      error.response?.status === 401 &&
      config &&
      !config._retry &&
      !config.url?.includes("/auth/")
    ) {
      config._retry = true;
      refreshingPromise ??= api
        .post("/auth/refresh")
        .then((res) => {
          setToken(res.data.accessToken);
          return res.data.accessToken;
        })
        .finally(() => {
          refreshingPromise = null;
        });

      try {
        await refreshingPromise;
        return api(config);
      } catch (refreshError) {
        setToken(null);
        window.dispatchEvent(new Event("auth:logout"));
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  }
);
