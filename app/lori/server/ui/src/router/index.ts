import { createRouter, createWebHistory, RouteRecordRaw } from "vue-router";

const routes: Array<RouteRecordRaw> = [
  {
    path: "/ui",
    name: "ui",
    component: () => import("../components/ItemList.vue"),
  },
  {
    path: "/",
    redirect: "/ui",
  },
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: routes,
});

export default router;
