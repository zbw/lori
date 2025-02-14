import { defineStore } from "pinia";
import { ref } from "vue";

/**
 * Store containing all dialog states.
 */
export const useDialogsStore = defineStore("dialog", () => {
  const bookmarkSaveActivated = ref(false);
  const bookmarkOverviewActivated = ref(false);
  const dashboardViewActivated = ref(false);
  const groupOverviewActivated = ref(false);
  const groupDeleteActivated = ref(false);
  const groupEditActivated = ref(false);
  const editRightActivated = ref(false);
  const rightsEditTabsActivated = ref(false);
  const rightsEditTabsSelectedRight = ref("");
  const templateOverviewActivated = ref(false);

  return {
    bookmarkOverviewActivated,
    bookmarkSaveActivated,
    dashboardViewActivated,
    editRightActivated,
    groupDeleteActivated,
    groupEditActivated,
    groupOverviewActivated,
    rightsEditTabsSelectedRight,
    rightsEditTabsActivated,
    templateOverviewActivated,
  };
});
