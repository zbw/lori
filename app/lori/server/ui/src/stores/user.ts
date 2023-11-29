import { defineStore } from "pinia";
import { ref } from "vue";

export const useUserStore = defineStore("user", () => {
  const emailAddress = ref("");
  const role = ref("");
  const isLoggedIn = ref(false);
  const signInURL = ref(
    "https://sso-1793b17c.sso.duosecurity.com/saml2/sp/DIMOJTPPL6E090X4BUL1/sso"
  );
  const signOutURL = ref(
    "https://sso-1793b17c.sso.duosecurity.com/saml2/sp/DIMOJTPPL6E090X4BUL1/slo"
  );

  return {
    emailAddress,
    isLoggedIn,
    signInURL,
    signOutURL,
    role,
  };
});
