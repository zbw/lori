import { defineStore } from "pinia";
import { ref } from "vue";

export const useUserStore = defineStore("user", () => {
  const emailAddress = ref("");
  const role = ref("");

  return {
    emailAddress,
    role,
  };
});
