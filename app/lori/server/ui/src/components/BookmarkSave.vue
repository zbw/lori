<script lang="ts">
import { computed, defineComponent, reactive, ref } from "vue";
import { useDialogsStore } from "@/stores/dialogs";
import bookmarkApi from "@/api/bookmarkApi";
import { useSearchStore } from "@/stores/search";
import searchquerybuilder from "@/utils/searchquerybuilder";
import error from "@/utils/error";
import { required } from "@vuelidate/validators";
import { useVuelidate } from "@vuelidate/core";
import {BookmarkIdCreated} from "@/generated-sources/openapi";

export default defineComponent({
  emits: ["addBookmarkSuccessful"],
  setup(props, { emit }) {
    /**
     * Constants:
     */
    const updateInProgress = ref(false);
    const description = ref("");
    /**
     * Vuelidate:
     */
    const rules = {
      name: { required },
    };
    const formState = reactive({
      name: "",
    });
    const v$ = useVuelidate(rules, formState);

    /**
     * Error handling:
     */
    const errorName = computed(() => {
      const errors: Array<string> = [];
      if (v$.value.name.$invalid && v$.value.name.$dirty) {
        errors.push("Es wird ein Name benötigt.");
      }
      return errors;
    });

    const saveAlertError = ref(false);
    const saveAlertErrorMessage = ref("");

    /**
     * Stores:
     */
    const dialogStore = useDialogsStore();
    const searchStore = useSearchStore();
    const close = () => {
      v$.value.$reset();
      updateInProgress.value = false;
      dialogStore.bookmarkSaveActivated = false;
      formState.name = "";
      description.value = "";
    };
    const save = () => {
      v$.value.$validate().then((isValid) => {
        if (!isValid) {
          return;
        }
        let bookmarkName = formState.name;
        updateInProgress.value = true;
        bookmarkApi
          .addRawBookmark(
            bookmarkName,
            description.value,
            searchStore.lastSearchTerm,
            searchquerybuilder.buildPublicationDateFilter(searchStore),
            searchquerybuilder.buildPublicationTypeFilter(searchStore),
            searchquerybuilder.buildAccessStateFilter(searchStore),
            searchquerybuilder.buildTempValFilter(searchStore),
            searchquerybuilder.buildStartDateAtFilter(searchStore),
            searchquerybuilder.buildEndDateAtFilter(searchStore),
            searchquerybuilder.buildFormalRuleFilter(searchStore),
            searchquerybuilder.buildValidOnFilter(searchStore),
            searchquerybuilder.buildPaketSigelIdFilter(searchStore),
            searchquerybuilder.buildZDBIdFilter(searchStore),
            searchquerybuilder.buildNoRightInformation(searchStore),
            searchquerybuilder.buildSeriesFilter(searchStore),
          )
          .then((r: BookmarkIdCreated) => {
            emit("addBookmarkSuccessful", r.bookmarkId, bookmarkName);
            close();
          })
          .catch((e) => {
            error.errorHandling(e, (errMsg: string) => {
              updateInProgress.value = false;
              saveAlertError.value = true;
              saveAlertErrorMessage.value = errMsg;
            });
          });
      });
    };
    return {
      description,
      dialogStore,
      errorName,
      formState,
      saveAlertError,
      saveAlertErrorMessage,
      updateInProgress,
      v$,
      close,
      save,
    };
  },
});
</script>

<style scoped></style>

<template>
  <v-card position="relative">
    <v-container>
      <v-card-title>Suche Speichern</v-card-title>
      <v-row>
        <v-col cols="4">Name</v-col>
        <v-col cols="8">
          <v-text-field
            v-model="formState.name"
            :error-messages="errorName"
            hint="Name des Bookmarks"
            maxlength="256"
            variant="outlined"
          ></v-text-field>
        </v-col>
      </v-row>
      <v-row>
        <v-col cols="4"> Bookmark-Id </v-col>
        <v-col cols="8">
          <v-text-field
            disabled
            label="Wird automatisch generiert"
            variant="outlined"
          ></v-text-field>
        </v-col>
      </v-row>
      <v-row>
        <v-col cols="4"> Beschreibung </v-col>
        <v-col cols="8">
          <v-text-field
            hint="Beschreibung des Bookmarks"
            v-model="description"
            outlined
          ></v-text-field>
        </v-col>
      </v-row>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn color="blue darken-1" text="Zurück" @click="close"></v-btn>
        <v-btn
          :disabled="updateInProgress"
          color="blue darken-1"
          text="Speichern"
          @click="save"
        ></v-btn>
      </v-card-actions>
      <v-snackbar
          contained
          multi-line
          location="top"
          timer="true"
          timeout="10000"
          v-model="saveAlertError"
          color="error"
      >
        {{ saveAlertErrorMessage }}
      </v-snackbar>
    </v-container>
  </v-card>
</template>
