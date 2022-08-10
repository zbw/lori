import Vue from "vue";
import App from "./App.vue";
import router from "./router";
import vuetify from "./plugins/vuetify";
import i18n from "./i18n";
import { createPinia, PiniaVuePlugin } from "pinia";

Vue.config.productionTip = false;
Vue.use(PiniaVuePlugin);
const pinia = createPinia();

new Vue({
  router,
  vuetify,
  i18n,
  pinia,
  render: (h) => h(App),
}).$mount("#app");
