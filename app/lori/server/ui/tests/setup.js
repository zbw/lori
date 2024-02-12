import { createApp } from "vue";
import vuetify from "@/plugins/vuetify";
import App from "@/App.vue";
//Vue.config.productionTip = false;
const app = createApp(App);
app.use(vuetify);
