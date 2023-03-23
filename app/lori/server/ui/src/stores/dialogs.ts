import { defineStore } from "pinia";
import { ref } from "vue";

/**
 * Store containing all dialog states.
 */
export const useDialogsStore = defineStore("dialog", () => {
  const bookmarkActivated = ref(false);
  const groupOverviewActivated = ref(false);
  const groupDeleteActivated = ref(false);
  const groupEditActivated = ref(false);
  const editRightActivated = ref(false);

  return {
    bookmarkActivated,
    editRightActivated,
    groupDeleteActivated,
    groupEditActivated,
    groupOverviewActivated,
  };
});
