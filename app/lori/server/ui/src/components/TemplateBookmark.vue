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
  setup(props) {
    /**
     * Stores.
     */
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
     * Error messages.
     */
    const bookmarkLoadError = ref(false);
    const bookmarkLoadErrorMsg = ref("");

    onMounted(() => getBookmarkList());
    const computedReinitCounter = computed(() => props.reinitCounter);
    watch(computedReinitCounter, () => {
      // Actions executed when the window is displayed:
      getBookmarkList();
      // TODO: probably reset previously selected bookmarks
    });

    return {
      headers,
      bookmarkItems,
      bookmarkLoadError,
      bookmarkLoadErrorMsg,
      searchTerm,
      selectedBookmarks,
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
    </v-container>
  </v-card>
</template>
