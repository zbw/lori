import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import vuetify from "./plugins/vuetify";
import { createPinia, Pinia } from "pinia";
import "material-design-icons-iconfont/dist/material-design-icons.css";

// TODO(CB): figure out meaning of option and add it if necessary
//Vue.config.productionTip = false;
const pinia: Pinia = createPinia();

const app = createApp(App);
app.use(router);
app.use(vuetify);
app.use(pinia);
app.mount("#app");


//import { createRouter, createWebHistory } from 'vue-router'
//import HomeView from '../views/HomeView.vue'
//
//const router = createRouter({
//    history: createWebHistory(import.meta.env.BASE_URL),
//    routes: [
//        {
//            path: '/',
//            name: 'home',
//            component: HomeView
//        },
//        {
//            path: '/about',
//            name: 'about',
//            // route level code-splitting
//            // this generates a separate chunk (About.[hash].js) for this route
//            // which is lazy-loaded when the route is visited.
//            component: () => import('../views/AboutView.vue')
//        }
//    ]
//})
//
//export default router
