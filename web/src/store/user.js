import { create } from 'zustand';

const TOKEN_KEY = 'token';
const USER_KEY = 'user';

/**
 * 全局登录状态。
 * 初始值直接从 localStorage 读 —— 所以刷新页面不会掉登录态。
 */
export const useUserStore = create((set) => ({
  token: localStorage.getItem(TOKEN_KEY) || '',
  user: JSON.parse(localStorage.getItem(USER_KEY) || 'null'),

  /** 登录成功后调用 */
  setLogin(token, user) {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    set({ token, user });
  },

  /** 用 /auth/me 拉到的最新快照刷新一下（比如管理员改了你的角色） */
  setUser(user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    set({ user });
  },

  /** 退出登录 */
  logout() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    set({ token: '', user: null });
  },
}));