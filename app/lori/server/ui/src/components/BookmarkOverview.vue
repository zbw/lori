<script lang="ts">
import { computed, defineComponent, onMounted, Ref, ref, watch } from "vue";
import { BookmarkRest, RightRest } from "@/generated-sources/openapi";
import bookmarkApi from "@/api/bookmarkApi";
import error from "@/utils/error";
import { useDialogsStore } from "@/stores/dialogs";
import RightsEditDialog from "@/components/RightsEditDialog.vue";

export default defineComponent({
  components: { RightsEditDialog },
  props: {},
  emits: ["executeBookmarkSearch"],
  setup(props, { emit }) {
    /**
     * Error messages.
     */
    const bookmarkError = ref(false);
    const bookmarkErrorMsg = ref("");

    /**
     * Alerts:
     */
    const alertSuccessful = ref(false);
    const alertSuccessfulMsg = ref("");

    /**
     * Data-Table related.
     */
    const renderKey = ref(0);
    const headers = [
      {
        title: "Id",
        key: "bookmarkId",
        align: "start",
        sortable: true,
      },
      {
        title: "Name",
        key: "bookmarkName",
        align: "start",
        sortable: true,
      },
      {
        title: "Template anlegen",
        key: "createTemplate",
        align: "start",
        sortable: false,
      },
      {
        title: "Suche ausführen",
        key: "executeSearch",
        align: "start",
        sortable: false,
      },
      {
        title: "Actions",
        key: "actions",
        align: "start",
        sortable: false,
      },
    ];
    const searchTerm = ref("");
    const bookmarkItems: Ref<Array<BookmarkRest>> = ref([]);
    const editBookmark = ref({} as BookmarkRest);
    const editIndex = ref(-1);
    const confirmationDialog = ref(false);
    const getBookmarkList = () => {
      bookmarkApi
        // TODO: Load entries dynamically
        .getBookmarkList(0, 200)
        .then((r: Array<BookmarkRest>) => {
          bookmarkItems.value = r;
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            bookmarkErrorMsg.value = errMsg;
            bookmarkError.value = true;
          });
        });
    };

    const deleteBookmarkEntry = () => {
      bookmarkApi
        .deleteBookmark(editBookmark.value.bookmarkId)
        .then(() => {
          bookmarkItems.value.splice(editIndex.value, 1);
          renderKey.value += 1;
          closeDeleteDialog();
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            bookmarkErrorMsg.value = errMsg;
            bookmarkError.value = true;
          });
          closeDeleteDialog();
        });
    };

    const openDeleteDialog = (bookmark: BookmarkRest) => {
      editBookmark.value = Object.assign({}, bookmark);
      editIndex.value = bookmarkItems.value.indexOf(bookmark);
      confirmationDialog.value = true;
    };

    const closeDeleteDialog = () => {
      confirmationDialog.value = false;
    };

    /**
     * Stores and their actions.
     */
    const dialogStore = useDialogsStore();
    const close = () => {
      dialogStore.bookmarkOverviewActivated = false;
      bookmarkError.value = false;
      bookmarkErrorMsg.value = "";
      confirmationDialog.value = false;
      editBookmark.value = {} as BookmarkRest;
      editIndex.value = -1;
      alertSuccessful.value = false;
      templateDialogActivated.value = false;
    };

    /**
     * Template.
     */
    const templateDialogActivated = ref(false);
    const templateReinitCounter = ref(0);
    const currentBookmark = ref({} as BookmarkRest);

    const activateTemplateDialog = (bookmark: BookmarkRest) => {
      currentBookmark.value = bookmark;
      templateReinitCounter.value += 1;
      templateDialogActivated.value = true;
    };
    const closeTemplateDialog = () => {
      templateDialogActivated.value = false;
    };

    const childTemplateAdded = (template: RightRest) => {
      alertSuccessful.value = true;
      alertSuccessfulMsg.value =
        "Successfully created Template " + template.templateName;
    };

    /**
     * Search.
     */
    const executeBookmarkSearch = (bookmark: BookmarkRest) => {
      emit("executeBookmarkSearch", bookmark);
    };

    /**
     * Mounted, computed and watch.
     */
    onMounted(() => getBookmarkList());

    const computedBookmarkOverview = computed(
      () => dialogStore.bookmarkOverviewActivated,
    );
    watch(computedBookmarkOverview, (currentValue) => {
      if (currentValue) {
        getBookmarkList();
      }
    });

    return {
      alertSuccessful,
      alertSuccessfulMsg,
      bookmarkError,
      bookmarkErrorMsg,
      bookmarkItems,
      confirmationDialog,
      currentBookmark,
      headers,
      renderKey,
      searchTerm,
      templateDialogActivated,
      templateReinitCounter,
      activateTemplateDialog,
      childTemplateAdded,
      close,
      closeDeleteDialog,
      closeTemplateDialog,
      deleteBookmarkEntry,
      executeBookmarkSearch,
      openDeleteDialog,
    };
  },
});
</script>

<style scoped></style>
<template>
  <v-card position="relative">
    <v-container>
      <v-snackbar
          contained
          multi-line
          location="top"
          timer="true"
          timeout="10000"
          v-model="bookmarkError"
          color="error"
      >
        {{ bookmarkErrorMsg }}
      </v-snackbar>
      <v-snackbar
          contained
          multi-line
          location="top"
          timer="true"
          timeout="10000"
          v-model="alertSuccessful"
          color="success"
      >
        {{ alertSuccessfulMsg }}
      </v-snackbar>
      <v-card-title>Gespeicherte Suchen verwalten</v-card-title>
      <v-dialog v-model="confirmationDialog" max-width="500px">
        <v-card>
          <v-card-title class="text-h5">Löschen bestätigen</v-card-title>
          <v-card-actions>
            <v-spacer></v-spacer>
            <v-btn
              color="blue darken-1"
              text="Abbrechen"
              @click="closeDeleteDialog"
            ></v-btn>
            <v-btn
              color="error"
              text="Löschen"
              @click="deleteBookmarkEntry"
            ></v-btn>
            <v-spacer></v-spacer>
          </v-card-actions>
        </v-card>
      </v-dialog>
      <v-text-field
        v-model="searchTerm"
        append-icon="mdi-magnify"
        hide-details
        label="Suche"
        single-line
      ></v-text-field>
      <v-data-table
        :key="renderKey"
        :headers="headers"
        :items="bookmarkItems"
        :search="searchTerm"
        item-value="bookmarkId"
        loading-text="Daten werden geladen... Bitte warten."
      >
        <template v-slot:item.createTemplate="{ item }">
          <v-btn
            color="blue darken-1"
            text="Template anlegen"
            @click="activateTemplateDialog(item)"
          ></v-btn>
        </template>
        <template v-slot:item.executeSearch="{ item }">
          <v-btn color="blue darken-1" @click="executeBookmarkSearch(item)"
            >Suche ausführen</v-btn
          >
        </template>
        <template v-slot:item.actions="{ item }">
          <v-icon small @click="openDeleteDialog(item)"> mdi-delete </v-icon>
        </template>
      </v-data-table>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn color="blue darken-1" @click="close">Zurück</v-btn>
      </v-card-actions>
      <v-dialog
        v-model="templateDialogActivated"
        :retain-focus="false"
        max-width="1000px"
        v-on:close="closeTemplateDialog"
      >
        <RightsEditDialog
          :index="-1"
          :initial-bookmark="currentBookmark"
          :isNewRight="false"
          :isNewTemplate="true"
          :reinit-counter="templateReinitCounter"
          v-on:addTemplateSuccessful="childTemplateAdded"
          v-on:editRightClosed="closeTemplateDialog"
        ></RightsEditDialog>
      </v-dialog>
    </v-container>
  </v-card>
</template>
