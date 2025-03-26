<script lang="ts">
import {computed, defineComponent, PropType, reactive, ref, watch} from "vue";
import { useDialogsStore } from "@/stores/dialogs";
import bookmarkApi from "@/api/bookmarkApi";
import { useSearchStore } from "@/stores/search";
import searchquerybuilder from "@/utils/searchquerybuilder";
import error from "@/utils/error";
import { required } from "@vuelidate/validators";
import { useVuelidate } from "@vuelidate/core";
import {BookmarkIdCreated, BookmarkRest, RightRest} from "@/generated-sources/openapi";

export default defineComponent({
  emits: [
    "addBookmarkSuccessful",
    "closeEditDialog",
    "editBookmarkSuccessful",
  ],
  props: {
    isNew: {
      type: Boolean,
      required: true,
    },
    bookmark: {
      type: Object as PropType<BookmarkRest>,
      required: false,
    },
    reinitCounter: {
      type: Number,
      required: false,
    },
  },
  setup(props, { emit }) {
    /**
     * Constants:
     */
    const updateInProgress = ref(false);
    const description = ref("");
    const query = ref("");
    const tmpBookmark = ref({} as BookmarkRest);
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
      emit("closeEditDialog");
    };

    /**
     * Changes:
     */
    const formWasChanged = computed(() => {
      return (props.isNew && (formState.name != "" ||
          description.value != "")) ||
          (!props.isNew && (
              formState.name != props.bookmark?.bookmarkName ||
              description.value != props.bookmark?.description
          ))
    });
    const unsavedChangesDialog = ref(false);
    const checkForChangesAndClose = () => {
      if(formWasChanged.value){
        unsavedChangesDialog.value = true;
      } else {
        close();
      }
    };

    const closeUnsavedChangesDialog = () => {
      unsavedChangesDialog.value = false;
    };

    const save = () => {
      v$.value.$validate().then((isValid) => {
        if (!isValid) {
          return;
        }
        updateInProgress.value = true;
        if(props.isNew){
          create();
        } else {
          update();
        }
      });
    };

    const create = () => {
      let bookmarkName = formState.name;
      bookmarkApi
        .addRawBookmark(
            bookmarkName,
            description.value,
            searchStore.lastSearchTerm,
            searchquerybuilder.buildPublicationYearFilter(searchStore),
            searchquerybuilder.buildPublicationTypeFilter(searchStore),
            searchquerybuilder.buildAccessStateFilter(searchStore),
            searchquerybuilder.buildStartDateAtFilter(searchStore),
            searchquerybuilder.buildEndDateAtFilter(searchStore),
            searchquerybuilder.buildFormalRuleFilter(searchStore),
            searchquerybuilder.buildValidOnFilter(searchStore),
            searchquerybuilder.buildPaketSigelIdFilter(searchStore),
            searchquerybuilder.buildZDBIdFilter(searchStore),
            searchquerybuilder.buildNoRightInformation(searchStore),
            searchquerybuilder.buildSeriesFilter(searchStore),
            searchquerybuilder.buildTemplateNameFilter(searchStore),
            searchquerybuilder.buildLicenceUrlFilter(searchStore),
            searchquerybuilder.buildManualRightFilter(searchStore),
            searchquerybuilder.buildAccessOnDateFilter(searchStore),
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
        }).finally(() =>{
          updateInProgress.value = false;
        }
      );
    };

    const update = () => {
      tmpBookmark.value = Object.assign({}, props.bookmark);
      tmpBookmark.value.bookmarkName = formState.name;
      tmpBookmark.value.description = description.value;
      bookmarkApi
          .updateBookmark(tmpBookmark.value)
          .then(() => {
            emit("editBookmarkSuccessful", { ...tmpBookmark.value });
            close();
          })
          .catch((e) => {
            error.errorHandling(e, (errMsg: string) => {
              updateInProgress.value = false;
              saveAlertError.value = true;
              saveAlertErrorMessage.value = errMsg;
            });
          }).finally(() => {
        updateInProgress.value = false;
      });
    };


    const cardTitle = computed(() => {
      const mode = props.isNew ? "speichern" : "bearbeiten";
      return "Suche " + mode;
    });

    const reinitializeBookmark = () => {
      if(!props.isNew){
        description.value = props.bookmark?.description ?? '';
        formState.name = props.bookmark?.bookmarkName ?? '';
        query.value = props.bookmark?.filtersAsQuery ?? '';
      }
    };

    const resetAllValues = () => {
      description.value = '';
      formState.name = '';
      query.value = '';
    };

    watch(
        () => props.reinitCounter,
        () => {
      if(props.isNew){
        resetAllValues();
      } else {
        reinitializeBookmark();
      }
    },
    { immediate: true }
    );

    return {
      cardTitle,
      description,
      dialogStore,
      errorName,
      formState,
      query,
      saveAlertError,
      saveAlertErrorMessage,
      unsavedChangesDialog,
      updateInProgress,
      v$,
      checkForChangesAndClose,
      closeUnsavedChangesDialog,
      close,
      save,
    };
  },
});
</script>

<style scoped></style>

<template>
  <v-card position="relative">
    <v-toolbar>
      <v-spacer></v-spacer>
      <v-btn
          icon="mdi-close"
          @click="checkForChangesAndClose"
      ></v-btn>
    </v-toolbar>
    <v-dialog v-model="unsavedChangesDialog" max-width="500px">
      <v-card>
        <v-card-title class="text-h5 text-center">Hinweis</v-card-title>
        <v-card-text class="text-center">
          Änderungen wurden noch nicht gespeichert!
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn
              @click="closeUnsavedChangesDialog"
              color="blue darken-1"
          >Abbrechen
          </v-btn>
          <v-btn
              color="error"
              @click="close">
            Änderungen verwerfen
          </v-btn>
          <v-spacer></v-spacer>
        </v-card-actions>
      </v-card>
    </v-dialog>
    <v-container>
      <v-card-title class="">{{ cardTitle }}</v-card-title>
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
        <v-col cols="4"> Beschreibung</v-col>
        <v-col cols="8">
          <v-textarea
            hint="Beschreibung des Bookmarks"
            v-model="description"
            outlined
          ></v-textarea>
        </v-col>
      </v-row>
      <v-row>
        <v-col cols="4"> Query</v-col>
        <v-col cols="8">
          <v-text-field
              v-model="query"
              bg-color="grey-lighten-2"
              readonly
              variant="outlined"
          ></v-text-field>
        </v-col>
      </v-row>
      <v-card-actions>
        <v-spacer></v-spacer>
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
          timeout="5000"
          v-model="saveAlertError"
          color="error"
      >
        {{ saveAlertErrorMessage }}
      </v-snackbar>
    </v-container>
  </v-card>
</template>
