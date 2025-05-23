<script lang="ts">
import {computed, defineComponent, onMounted, ref} from "vue";
import { useDialogsStore } from "@/stores/dialogs";
import usersApi from "@/api/usersApi";
import error from "@/utils/error";
import { useCookies } from "vue3-cookies";
import { UserSessionRest } from "@/generated-sources/openapi";
import { useUserStore } from "@/stores/user";
import GroupOverview from "@/components/GroupOverview.vue";
import {useSearchStore} from "@/stores/search";

export default defineComponent({
  components: {GroupOverview},
  setup() {
    const cookies = useCookies();
    const searchStore = useSearchStore();
    const menuTopics = [{ title: "IP-Gruppen" }, { title: "Einstellungen" }];
    const dialogStore = useDialogsStore();
    const activateGroupDialog = () => {
      dialogStore.groupOverviewActivated = true;
    };
    const activateTemplateDialog = () => {
      dialogStore.templateOverviewActivated = true;
    };

    const activateDashboardDialog = () => {
      dialogStore.dashboardViewActivated = true;
    };

    const activateBookmarkOverviewDialog = () => {
      dialogStore.bookmarkOverviewActivated = true;
    };

    /**
     * LOGIN
     */
    const userStore = useUserStore();
    const loginError = ref(false);
    const loginErrorMsg = ref("");
    const loginErrorMsgTitle = ref("");
    const loginSuccessful = ref(false);
    const loginSuccessfulMsg = ref("");
    const loginUnauthorized = ref(false);
    const cookieName = "JSESSIONID";
    const login = (init: boolean) => {
      const cookieValue = cookies.cookies.isKey(cookieName)
        ? cookies.cookies.get(cookieName)
        : "none";
      usersApi
        .getSessionById(cookieValue)
        .then((userSession: UserSessionRest) => {
          userStore.emailAddress = userSession.email;
          userStore.permissions = userSession.permissions;
          userStore.isLoggedIn = true;
          if (!init) {
            loginSuccessful.value = true;
            loginSuccessfulMsg.value =
              "You are successfully logged in as " + userSession.email;
          }
        })
        .catch((e) => {
          error.errorHandling(
            e,
            (errMsg: string, errorCode: string, errorDetail: string) => {
              userStore.isLoggedIn = false;
              console.log("Error Code: " + errorCode);
              if (!init) {
                if (errorCode == "401") {
                  loginUnauthorized.value = true;
                } else {
                  loginErrorMsgTitle.value = "Login war nicht erfolgreich";
                  loginErrorMsg.value = errMsg;
                  loginError.value = true;
                }
              }
            },
          );
        });
    };
    const deactivateLoginDialog = () => {
      loginUnauthorized.value = false;
    };

    const logoutDialog = ref(false);
    const logout = () => {
      const cookieValue = cookies.cookies.isKey(cookieName)
        ? cookies.cookies.get(cookieName)
        : "none";
      usersApi
        .deleteSessionById(cookieValue)
        .then(() => {
          userStore.emailAddress = "";
          userStore.permissions = undefined;
          userStore.isLoggedIn = false;
          logoutDialog.value = true;
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            loginErrorMsgTitle.value = "Logout war nicht erfolgreich";
            loginErrorMsg.value = errMsg;
            loginError.value = true;
          });
        });
    };

    const deactivateLogoutDialog = () => {
      logoutDialog.value = false;
    };

    const closeGroupDialog = () => {
      dialogStore.groupOverviewActivated = false;
    };

    const appBarColor = computed(() => {
      switch (searchStore.stage) {
        case 'prod':
          return "#1565C0";
        case 'dev':
          return "#EB6B05";
        default:
          return "#FFFFFF"; // default or loading state
      }
    });

    onMounted(() => login(true));
    return {
      appBarColor,
      dialogStore,
      loginError,
      loginErrorMsg,
      loginErrorMsgTitle,
      loginSuccessful,
      loginSuccessfulMsg,
      loginUnauthorized,
      logoutDialog,
      menuTopics,
      searchStore,
      userStore,
      activateBookmarkOverviewDialog,
      activateDashboardDialog,
      activateGroupDialog,
      activateTemplateDialog,
      closeGroupDialog,
      deactivateLoginDialog,
      deactivateLogoutDialog,
      login,
      logout,
    };
  },
});
</script>

<style scoped>
</style>
<template>
  <v-dialog
      v-model="dialogStore.groupOverviewActivated"
      :retain-focus="false"
      max-width="1000px"
      v-on:close="closeGroupDialog"
      persistent
  >
    <GroupOverview
        v-on:groupOverviewClosed="closeGroupDialog">
    </GroupOverview>
  </v-dialog>
  <v-app-bar
      app
      :color="appBarColor"
      >
    <div class="d-flex align-center">
      <v-img
        alt="Lori Logo"
        class="shrink mr-2"
        contain
        src="@/assets/LogoLori.png"
        transition="scale-transition"
        width="100"
      />
    </div>

    <v-menu :location="'bottom'">
      <template v-slot:activator="{ props }">
        <v-btn dark icon="mdi-view-headline" v-bind="props"> </v-btn>
      </template>

      <v-list>
        <v-list-item link>
          <v-list-item-title @click="activateDashboardDialog"
          >Dashboard</v-list-item-title
          >
        </v-list-item>
        <v-list-item link>
          <v-list-item-title @click="activateGroupDialog"
            >IP-Gruppen</v-list-item-title
          >
        </v-list-item>
        <v-list-item link>
          <v-list-item-title @click="activateTemplateDialog"
            >Templates</v-list-item-title
          >
        </v-list-item>
        <v-list-item link>
          <v-list-item-title @click="activateBookmarkOverviewDialog"
            >Gespeicherte Suchen</v-list-item-title
          >
        </v-list-item>
      </v-list>
    </v-menu>
    <div v-if="searchStore.stage == 'dev'">
      Testsystem
    </div>
    <v-spacer></v-spacer>
    <v-menu :location="'bottom'">
      <template v-slot:activator="{ props }">
        <v-btn class="mx-2" fab large color="purple" v-bind="props">
          <v-icon dark>mdi-account</v-icon>
        </v-btn>
      </template>
      <v-list v-if="!userStore.isLoggedIn" class="cursor-pointer">
        <v-hover>
          <template v-slot:default="{isHovering, props }">
            <v-list-item
                v-bind="props"
                :key="1"
                :value="1"
                :base-color="isHovering ? 'primary' : undefined"
            >
              <v-list-item-title @click="login(false)">Login</v-list-item-title>
            </v-list-item>
          </template>
        </v-hover>
      </v-list>
      <v-list v-if="userStore.isLoggedIn" class="cursor-pointer pa-0">
        <!-- First list item: User email -->
        <v-hover>
          <template v-slot:default="{ isHovering, props }">
            <v-list-item
                v-bind="props"
                :key="1"
                :value="1"
                :base-color="isHovering ? 'primary' : undefined"
            >
              <v-list-item-title>{{ userStore.emailAddress }}</v-list-item-title>
            </v-list-item>
          </template>
        </v-hover>

        <!-- Second list item: Logout button -->
        <v-hover>
          <template v-slot:default="{ isHovering, props }">
            <v-list-item
                v-bind="props"
                :key="2"
                :value="2"
                :base-color="isHovering ? 'primary' : undefined"
            >
              <v-list-item-title @click="logout">Logout</v-list-item-title>
            </v-list-item>
          </template>
        </v-hover>
      </v-list>
    </v-menu>
    <v-dialog v-model="loginError" max-width="290">
      <v-card>
        <v-card-title class="text-h5">
          Login war nicht erfolgreich
          {{ loginErrorMsgTitle }}
        </v-card-title>
        <v-card-text>{{ loginErrorMsg }}</v-card-text>
      </v-card>
    </v-dialog>
    <v-dialog v-model="loginUnauthorized" max-width="290">
      <v-card>
        <v-card-title class="text-h5"> Nicht authentifiziert </v-card-title>
        <v-card-text>
          Bitte <a :href="userStore.signInURL">hier</a> klicken zum Login
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn small color="primary" dark @click="deactivateLoginDialog">
            Ohne Login fortfahren
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
    <v-dialog v-model="logoutDialog" max-width="290">
      <v-card>
        <v-card-title class="text-h5">Logout</v-card-title>
        <v-card-text>
          Bitte <a :href="userStore.signOutURL" target="_blank">hier</a> klicken zum Logout
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn small color="primary" dark @click="deactivateLogoutDialog">
            Abbrechen
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
    <v-dialog v-model="loginSuccessful" max-width="290">
      <v-card>
        <v-card-title class="text-h5"> Login war erfolgreich </v-card-title>
        <v-card-text>{{ loginSuccessfulMsg }}</v-card-text>
      </v-card>
    </v-dialog>
  </v-app-bar>
</template>
