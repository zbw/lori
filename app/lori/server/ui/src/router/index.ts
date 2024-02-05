import { createRouter, createWebHistory, RouteRecordRaw } from "vue-router";

const routes: Array<RouteRecordRaw> = [
  {
    path: "/ui",
    name: "ui",
    component: () => import("../components/ItemList.vue"),
  },
  {
    path: "/api/v1",
    name: "api",
    component: () => import("../components/API.vue"),
  },
];

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes: routes,
});

export default router;
