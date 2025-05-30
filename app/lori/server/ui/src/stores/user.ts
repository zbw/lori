import { defineStore } from "pinia";
import { ref } from "vue";
import { UserPermissionRest } from "@/generated-sources/openapi";

export const useUserStore = defineStore("user", () => {
  const emailAddress = ref("");
  const permissions = ref([] as Array<UserPermissionRest> | undefined);
  const isLoggedIn = ref(false);
  const signInURL = ref( "");
  const signOutURL = ref("");
  const commitHash = ref("");

  return {
    commitHash,
    emailAddress,
    isLoggedIn,
    signInURL,
    signOutURL,
    permissions,
  };
});
