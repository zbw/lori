<script lang="ts">
import {computed, defineComponent, onMounted, ref, Ref, watch} from "vue";
import {RightRest} from "@/generated-sources/openapi";
import error from "@/utils/error";
import templateApi from "@/api/templateApi";

export default defineComponent({
  props: {
    reinitCounter: {
      type: Number,
      required: false,
    },
    rightId: {
      type: String,
      required: false,
    }
  },
  emits: ["exceptionSelected", "exceptionConnectClosed"],
  setup(props, { emit }) {
    const headers = [
      {
        title: "Name",
        align: "start",
        value: "templateName",
        sortable: true,
      },
    ];

    const templateItems: Ref<Array<RightRest>> = ref([]);
    const searchTerm = ref("");
    const selectedTemplates: Ref<Array<RightRest>> = ref([]);
    const getTemplateList = () => {
      templateApi
          .getTemplateList(
              0,
              100,
              true,
              false,
              props.rightId,
              false,
          ) // TODO: simplification for now
          .then((r: Array<RightRest>) => {
            templateItems.value = r;
          })
          .catch((e) => {
            error.errorHandling(e, (errMsg: string) => {
              errorMsg.value = errMsg;
              errorMsgIsActive.value = true;
            });
          });
    };

    const selectedRowColor = (row: any) => {
      if(selectedTemplates.value[0] !== undefined && selectedTemplates.value[0].rightId == row.item.rightId){
        return { class: "bg-blue-lighten-4"}
      }
    };

    const addActiveItem = (mouseEvent: MouseEvent, row: any) => {
      const bookmark: RightRest | undefined = templateItems.value.find(
          (e) => e.rightId === row.item.rightId,
      );
      if (bookmark !== undefined) {
        selectedTemplates.value = [row.item];
      }
    };

    /**
     * On Close & Save
     */
    const close = () => {
      emit("exceptionConnectClosed");
    };

    const save = () => {
      emit("exceptionSelected", selectedTemplates.value);
      close();
    };

    /**
     * Error messages.
     */
    const errorMsgIsActive = ref(false);
    const errorMsg = ref("");

    onMounted(() => getTemplateList());
    const computedReinitCounter = computed(() => props.reinitCounter);
    watch(computedReinitCounter, () => {
      // Actions executed when the window is displayed:
      selectedTemplates.value = [];
      getTemplateList();
    });

    return {
      headers,
      errorMsgIsActive,
      errorMsg,
      searchTerm,
      selectedTemplates,
      templateItems,
      addActiveItem,
      close,
      save,
      getTemplateList,
      selectedRowColor,
    };
  },
});
</script>

<template>
  <v-card position="relative">
    <v-container>
      <v-card-title>Auswahl Ausnahme</v-card-title>
      <v-snackbar
          contained
          multi-line
          location="top"
          timer="true"
          timeout="5000"
          v-model="errorMsgIsActive"
          color="error"
      >
        {{ errorMsg }}
      </v-snackbar>
      <v-text-field
          v-model="searchTerm"
          append-icon="mdi-magnify"
          hide-details
          label="Suche"
          single-line
      ></v-text-field>
      <v-data-table
          v-model="selectedTemplates"
          :headers="headers"
          :items="templateItems"
          :search="searchTerm"
          :row-props="selectedRowColor"
          item-value="bookmarkId"
          select-strategy="single"
          return-object
          @click:row="addActiveItem"
      >
      </v-data-table>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn color="blue darken-1" text="Zurück" @click="close"></v-btn>
        <v-btn
            :disabled="selectedTemplates.length == 0"
            color="blue darken-1"
            text="Speichern"
            @click="save"
        >
        </v-btn>
      </v-card-actions>
    </v-container>
  </v-card>
</template>
<style scoped>
</style>