import { defineStore } from "pinia";

const STORAGE_KEY = "cd_auth";

function readStored() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

function writeStored(value) {
  if (!value) {
    localStorage.removeItem(STORAGE_KEY);
    return;
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(value));
}

export const useAuthStore = defineStore("auth", {
  state: () => ({
    session: readStored()
  }),
  getters: {
    isLoggedIn: state => Boolean(state.session?.role),
    role: state => state.session?.role || null,
    displayName: state => state.session?.name || ""
  },
  actions: {
    setSession(session) {
      this.session = session;
      writeStored(session);
    },
    logout() {
      this.session = null;
      writeStored(null);
    }
  }
});

