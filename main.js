
import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import * as Icons from "@element-plus/icons-vue";
import "./styles/theme.css";
import { pinia } from "./stores/pinia";

const app = createApp(App);
Object.entries(Icons).forEach(([name, comp]) => {
  app.component(name, comp);
});
app.use(pinia).use(router).use(ElementPlus).mount("#app");
