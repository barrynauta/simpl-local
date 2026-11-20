import { createApp } from "vue";
import App from "./App.vue";
import { login } from "./auth";
import { router } from "./router";
import "./style.css";

login()
  .then(() => {
    createApp(App).use(router).mount("#app");
  })
  .catch(() => {
    const root = document.getElementById("app");
    if (root) {
      root.textContent = "The login service is not available. Retry later.";
    }
  });
