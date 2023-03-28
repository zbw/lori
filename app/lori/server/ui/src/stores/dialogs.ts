import { defineStore } from "pinia";
import { ref } from "vue";

/**
 * Store containing all dialog states.
 */
export const useDialogsStore = defineStore("dialog", () => {
  const bookmarkSaveActivated = ref(false);
  const groupOverviewActivated = ref(false);
  const groupDeleteActivated = ref(false);
  const groupEditActivated = ref(false);
  const editRightActivated = ref(false);

  return {
    bookmarkSaveActivated,
    editRightActivated,
    groupDeleteActivated,
    groupEditActivated,
    groupOverviewActivated,
  };
});
