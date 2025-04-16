import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import vuetify from "./plugins/vuetify";
import { createPinia, Pinia } from "pinia";
import "material-design-icons-iconfont/dist/material-design-icons.css";

const pinia: Pinia = createPinia();

const app = createApp(App);
app.use(router);
app.use(vuetify);
app.use(pinia);
app.mount("#app");