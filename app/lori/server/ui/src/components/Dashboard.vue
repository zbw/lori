<script lang="ts">


import {defineComponent, onMounted, Ref, ref} from "vue";
import {RightErrorRest, RightRest} from "@/generated-sources/openapi";
import error from "@/utils/error";
import rightErrorApi from "@/api/rightErrorApi";

export default defineComponent({
  components: {},
  props: {},
  emits: [],
  setup(props, {emit}) {
    /**
     * Table:
     */
    const renderKey = ref(0);
    const errorItems: Ref<Array<RightErrorRest>> = ref([]);
    const headers = [
      {
        title: "Handle",
        align: "start",
        value: "handleId",
        sortable: true,
      },
      {
        title: "Art",
        align: "start",
        value: "conflictType",
        sortable: true,
      },
      {
        title: "Erzeugt durch",
        value: "conflictByTemplateName",
        sortable: true,
      },
      {
        title: "Anlass",
        value: "conflictingWithRightId",
        sortable: true,
      },
      {
        title: "Erzeugt am",
        value: "createdOn",
        sortable: true
      },
    ];

    const getErrorList = () => {
      rightErrorApi
          .getRightErrorList(0, 100)
          .then((r: Array<RightErrorRest>) => {
            errorItems.value = r;
          })
          .catch((e) => {
            error.errorHandling(e, (errMsg: string) => {
              errorMsg.value = errMsg;
              errorMsgIsActive.value = true;
            });
          });
    };
    const searchTerm = ref("");

    const createHandleHref = (handleId : string) => {
      return window.location.origin + window.location.pathname +  "?dashboardHandleSearch=hdl:" + handleId;
    };

    const createTemplateHref = (rightId : string) => {
      return window.location.origin + window.location.pathname +  "?templateId=" + rightId;
    };
    /**
     * Alerts:
     */
    const errorMsgIsActive = ref(false);
    const errorMsg = ref("");
    const successMsgIsActive = ref(false);
    const successMsg = ref("");

    onMounted(() => getErrorList());

    return {
      headers,
      errorItems,
      errorMsg,
      errorMsgIsActive,
      renderKey,
      searchTerm,
      successMsg,
      successMsgIsActive,
      createHandleHref,
      createTemplateHref,
      getErrorList,
    };
  },
});
</script><style scoped>

</style>

<template>
  <v-card position="relative">
    <v-container>
      <v-card-title class="text-h5"
      >Konflikte Zeitliche Gültigkeit</v-card-title
      >
      <v-snackbar
          contained
          multi-line
          location="top"
          timer="true"
          timeout="10000"
          v-model="successMsgIsActive"
          color="success"
      >
        {{ successMsg }}
      </v-snackbar>
      <v-snackbar
          contained
          multi-line
          location="top"
          timer="true"
          timeout="10000"
          v-model="errorMsgIsActive"
          color="error"
      >
        {{ errorMsg }}
      </v-snackbar>
      <v-data-table
          :key="renderKey"
          :headers="headers"
          :items="errorItems"
          :search="searchTerm"
          item-value="handleId"
          loading-text="Daten werden geladen... Bitte warten."
      >
      <template v-slot:item.conflictByTemplateName="{ item }">
        <td >
          <a
              v-bind:href="
                  createTemplateHref(item.conflictByRightId)
                  "
              target="_blank"
          > Template '{{item.conflictByTemplateName}}'</a>
        </td>
      </template>
      <template v-slot:item.conflictType="{ item }">
        <td >Widerspruch
        </td>
      </template>
        <template v-slot:item.handleId="{ item }">
          <td>
            <a
                v-bind:href="
                  createHandleHref(item.handleId)
                  "
                target="_blank"
            > {{ item.handleId}}</a>
          </td>
        </template>
        <template v-slot:item.conflictingWithRightId="{ item }">
          <td class="cursor-pointer">
            Right-ID: {{ item.conflictingWithRightId }}
          </td>
        </template>
      </v-data-table>
    </v-container>
  </v-card>
</template>

