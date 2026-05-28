
import { createRouter, createWebHistory } from "vue-router";
import HomeView from "../views/HomeView.vue";
import LoginView from "../views/LoginView.vue";
import OrderSubmit from "../views/OrderSubmit.vue";
import OrderQuery from "../views/OrderQuery.vue";
import AdminDashboard from "../views/AdminDashboard.vue";
import OrderManage from "../views/OrderManage.vue";
import PathPlanning from "../views/PathPlanning.vue";
import VehicleManage from "../views/VehicleManage.vue";
import { useAuthStore } from "../stores/auth";
import { pinia } from "../stores/pinia";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", redirect: "/login" },
    { path: "/login", name: "login", component: LoginView },
    { path: "/home", name: "home", component: HomeView, meta: { auth: "any" } },
    { path: "/order/submit", name: "order-submit", component: OrderSubmit, meta: { auth: "user" } },
    { path: "/order/query", name: "order-query", component: OrderQuery, meta: { auth: "user" } },
    { path: "/admin/login", redirect: "/login" },
    { path: "/admin", name: "admin-dashboard", component: AdminDashboard, meta: { auth: "admin" } },
    { path: "/admin/orders", name: "order-manage", component: OrderManage, meta: { auth: "admin" } },
    { path: "/admin/path", name: "path-planning", component: PathPlanning, meta: { auth: "admin" } },
    { path: "/admin/vehicles", name: "vehicle-manage", component: VehicleManage, meta: { auth: "admin" } }
  ]
});

router.beforeEach((to) => {
  const auth = useAuthStore(pinia);
  const required = to.meta?.auth;
  if (!required) return true;
  if (!auth.isLoggedIn) return { path: "/login" };
  if (required === "any") return true;
  if (required === auth.role) return true;
  return { path: "/login" };
});

export default router;
