import { createRouter, createWebHistory, RouteRecordRaw } from "vue-router";
import RestApi from "@/views/RestApi.vue";

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
  {
    path: "/rest",
    name: "RestApi",
    component: RestApi,
  },
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: routes,
});

export default router;
