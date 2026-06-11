<template>
  <div id="redoc-container" style="min-height: 100vh" />
</template>

<script lang="ts">
import { defineComponent, onMounted, nextTick } from "vue";
import openapiSpec from "@/generated-sources/openapijson/openapi.json";
import api from "@/api/api";
import { AboutRest } from "@/generated-sources/openapi";

export default defineComponent({
  name: "RestApi",

  setup() {
    const loadBackendParameters = () => {
      api
        .getAboutInformation()
        .then((response: AboutRest) => {
          if (response.stage == "dev") {
            document.title = "lori-dev";
          }
          if (response.stage == "qs") {
            document.title = "lori-qs";
          }
        })
        .catch((e) => {
          console.error("Could not reach the backend");
        });
    };
    onMounted(async () => {
      loadBackendParameters();
      await nextTick(); // Ensure DOM is fully rendered

      const el = document.getElementById("redoc-container");

      if (el && window.Redoc) {
        window.Redoc.init(
          openapiSpec,
          {
            scrollYOffset: 50,
          },
          el,
        );
      } else {
        console.error("Redoc or #redoc-container not found");
      }
    });
  },
});
</script>
