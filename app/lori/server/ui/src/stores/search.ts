import { defineStore } from "pinia";
import { computed, ref } from "vue";

export const useSearchStore = defineStore("search", () => {
  const lastSearchTerm = ref("");

  const publicationDateFrom = ref("");
  const publicationDateTo = ref("");

  return {
    lastSearchTerm,
    publicationDateFrom,
    publicationDateTo,
  };
});
