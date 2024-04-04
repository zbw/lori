import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import vuetify = require("vite-plugin-vuetify");

// https://vitejs.dev/config/

const path = require("path");
export default defineConfig({
  plugins: [vue(), vuetify({ autoImport: true })],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    proxy: {
      "/api": "http://localhost:8082",
    },
  },
});
