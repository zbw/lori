<script lang="ts">
import { computed, defineComponent, onMounted, Ref, ref, watch } from "vue";
import bookmarkApi from "@/api/bookmarkApi";
import { BookmarkRest } from "@/generated-sources/openapi";
import error from "@/utils/error";

export default defineComponent({
  props: {
    reinitCounter: {
      type: Number,
      required: false,
    },
  },
  emits: ["bookmarksSelected", "templateBookmarkClosed"],
  setup(props, { emit }) {
    const headers = [
      {
        text: "Name",
        align: "start",
        value: "bookmarkName",
        sortable: true,
      },
      {
        text: "Id",
        value: "bookmarkId",
        sortable: true,
      },
    ];

    const bookmarkItems: Ref<Array<BookmarkRest>> = ref([]);
    const searchTerm = ref("");
    const selectedBookmarks = ref([]);
    const getBookmarkList = () => {
      bookmarkApi
        .getBookmarkList(0, 100) // TODO: simplification for now
        .then((r: Array<BookmarkRest>) => {
          bookmarkItems.value = r;
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            bookmarkLoadErrorMsg.value = errMsg;
            bookmarkLoadError.value = true;
          });
        });
    };

    /**
     * On Close & Save
     */
    const close = () => {
      emit("templateBookmarkClosed");
    };

    const save = () => {
      emit("bookmarksSelected", selectedBookmarks.value);
      close();
    };

    /**
     * Error messages.
     */
    const bookmarkLoadError = ref(false);
    const bookmarkLoadErrorMsg = ref("");

    onMounted(() => getBookmarkList());
    const computedReinitCounter = computed(() => props.reinitCounter);
    watch(computedReinitCounter, () => {
      // Actions executed when the window is displayed:
      selectedBookmarks.value = [];
      getBookmarkList();
    });

    return {
      headers,
      bookmarkItems,
      bookmarkLoadError,
      bookmarkLoadErrorMsg,
      searchTerm,
      selectedBookmarks,
      close,
      save,
      getBookmarkList,
    };
  },
});
</script>

<style scoped></style>
<template>
  <v-card>
    <v-container>
      <v-card-title>Suche Gespeichterte Suchen</v-card-title>
      <v-alert v-model="bookmarkLoadError" dismissible text type="error">
        {{ bookmarkLoadErrorMsg }}
      </v-alert>
      <v-text-field
        v-model="searchTerm"
        append-icon="mdi-magnify"
        hide-details
        label="Suche"
        single-line
      ></v-text-field>
      <v-data-table
        v-model="selectedBookmarks"
        :headers="headers"
        :items="bookmarkItems"
        :search="searchTerm"
        item-key="bookmarkId"
        show-select
      >
      </v-data-table>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn color="blue darken-1" text @click="close">Zurück</v-btn>
        <v-btn
          :disabled="selectedBookmarks.length == 0"
          color="blue darken-1"
          text
          @click="save"
          >Speichern
        </v-btn>
      </v-card-actions>
    </v-container>
  </v-card>
</template>
