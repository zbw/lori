import { defineStore } from "pinia";
import { ref } from "vue";

/**
 * Store containing all dialog states.
 */
export const useDialogsStore = defineStore("dialog", () => {
  const groupOverviewActivated = ref(false);
  const editRightActivated = ref(false);

  return {
    editRightActivated,
    groupOverviewActivated,
  };
});
