<script lang="ts">
import { onMounted } from "vue";
import { defineComponent } from "vue";
import OpenApi from "vue-openapi";
import jsonApi from "@/generated-sources/openapijson/openapi.json";

export default defineComponent({
  components: {
    OpenApi,
  },
  setup() {
    const queryParams = { };

    const printDebug = () => {
      console.log(jsonApi);
    };

    onMounted(() => printDebug());
    return {
      jsonApi,
      queryParams,
      printDebug,
    };
  },
});
</script>

<style src="vue-openapi/node_modules/vue-material/dist/vue-material.css"></style>

<template>
  <open-api
    :api="jsonApi"
    md-theme="\'default\'"
    :query-params="queryParams"
  ></open-api>
</template>
