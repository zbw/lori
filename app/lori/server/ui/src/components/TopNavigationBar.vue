<script lang="ts">
import { defineComponent, onMounted, ref } from "vue";
import { useHistoryStore } from "@/stores/history";
import { useDialogsStore } from "@/stores/dialogs";
import usersApi from "@/api/usersApi";
import error from "@/utils/error";
import { useCookies } from "vue3-cookies";
import { UserSessionRest } from "@/generated-sources/openapi";
import { useUserStore } from "@/stores/user";

export default defineComponent({
  setup() {
    const historyStore = useHistoryStore();
    const cookies = useCookies();
    const menuTopics = [{ title: "IP-Gruppen" }, { title: "Einstellungen" }];
    const dialogStore = useDialogsStore();
    const activateGroupDialog = () => {
      dialogStore.groupOverviewActivated = true;
    };
    const activateTemplateDialog = () => {
      dialogStore.templateOverviewActivated = true;
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
      let cookieValue = cookies.cookies.isKey(cookieName)
        ? cookies.cookies.get(cookieName)
        : "none";
      usersApi
        .getSessionById(cookieValue)
        .then((userSession: UserSessionRest) => {
          userStore.emailAddress = userSession.email;
          userStore.role = userSession.role;
          userStore.isLoggedIn = true;
          if (!init) {
            loginSuccessful.value = true;
            loginSuccessfulMsg.value =
              "You are successfully logged in as " + userSession.email;
          }
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string, errorCode: string) => {
            userStore.isLoggedIn = false;
            if (!init) {
              if (errorCode == "401") {
                loginUnauthorized.value = true;
              } else {
                loginErrorMsgTitle.value = "Login war nicht erfolgreich";
                loginErrorMsg.value = errMsg;
                loginError.value = true;
              }
            }
          });
        });
    };
    const deactivateLoginDialog = () => {
      loginUnauthorized.value = false;
    };

    const logoutDialog = ref(false);
    const logout = () => {
      let cookieValue = cookies.cookies.isKey(cookieName)
        ? cookies.cookies.get(cookieName)
        : "none";
      usersApi
        .deleteSessionById(cookieValue)
        .then(() => {
          userStore.emailAddress = "";
          userStore.role = "";
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

    onMounted(() => login(true));
    return {
      dialogStore,
      historyStore,
      loginError,
      loginErrorMsg,
      loginErrorMsgTitle,
      loginSuccessful,
      loginSuccessfulMsg,
      loginUnauthorized,
      logoutDialog,
      menuTopics,
      userStore,
      activateBookmarkOverviewDialog,
      activateGroupDialog,
      activateTemplateDialog,
      deactivateLoginDialog,
      deactivateLogoutDialog,
      login,
      logout,
    };
  },
});
</script>

<style scoped></style>
<template>
  <v-app-bar app color="primary" dark>
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

    <v-menu :offset-y="true" bottom left>
      <template v-slot:activator="{ on, attrs }">
        <v-btn dark icon v-bind="attrs" v-on="on">
          <v-icon>mdi-view-headline</v-icon>
        </v-btn>
      </template>

      <v-list>
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
            >Bookmarks</v-list-item-title
          >
        </v-list-item>
        <v-list-item link>
          <v-list-item-title
            >Templates mit Bookmarks verknüpfen</v-list-item-title
          >
        </v-list-item>
      </v-list>
    </v-menu>

    <v-spacer></v-spacer>
    <v-menu offset-y>
      <template v-slot:activator="{ on, attrs }">
        <v-chip
          class="ma-2"
          color="green"
          text-color="white"
          v-bind="attrs"
          v-on="on"
        >
          <v-avatar class="green darken-4" left>
            {{ historyStore.numberEntries }}
          </v-avatar>
          Änderungen
        </v-chip>
      </template>
      <v-list>
        <v-list-item v-for="(item, index) in historyStore.history" :key="index">
          <v-list-item-action>
            <v-btn color="primary" depressed fab small>
              {{ index }}
            </v-btn>
          </v-list-item-action>
          <v-list-item-title
            >{{ item.type.toString() }}: Right-Id
            {{ item.rightId }}
          </v-list-item-title>
        </v-list-item>
      </v-list>
    </v-menu>
    <v-menu offset-y>
      <template v-slot:activator="{ on, attrs }">
        <v-btn class="mx-2" fab large color="purple" v-bind="attrs" v-on="on">
          <v-icon dark>mdi-account</v-icon>
        </v-btn>
      </template>
      <v-list v-if="!userStore.isLoggedIn">
        <v-list-item :key="0">
          <v-list-item-title @click="login(false)">Login</v-list-item-title>
        </v-list-item>
      </v-list>
      <v-list v-if="userStore.isLoggedIn">
        <v-list-item :key="1">
          <v-list-item-title>{{ userStore.emailAddress }}</v-list-item-title>
        </v-list-item>
        <v-list-item :key="2">
          <v-list-item-title @click="logout">Logout</v-list-item-title>
        </v-list-item>
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
          Bitte <a :href="userStore.signOutURL">hier</a> klicken zum Logout
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
