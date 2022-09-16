import { defineStore } from "pinia";
import { computed, ref } from "vue";

export const useSearchStore = defineStore("search", () => {
  const lastSearchTerm = ref("");

  return {
    lastSearchTerm,
  };
});
