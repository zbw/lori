<script lang="ts">
import { computed, defineComponent, ref, watch } from "vue";
import { useSearchStore } from "@/stores/search";
import { useVuelidate } from "@vuelidate/core";
import { useDialogsStore } from "@/stores/dialogs";
import date_utils from "@/utils/date_utils";
import metadata_utils from "@/utils/metadata_utils";
import api from "@/api/api";
import {ItemInformation} from "@/generated-sources/openapi";
import error from "@/utils/error";

export default defineComponent({
  emits: ["startEmptySearch", "startSearch"],
  setup(props, { emit }) {
    const searchStore = useSearchStore();
    const temporalEvent = -1;

    const tempEventMenu = ref(false);
    const tempValidOnMenu = ref(false);
    type FormState = {
      tempEventInput: Date | undefined;
      tempEventStart: boolean;
      tempEventEnd: boolean;
    };

    const tempEventCheckForInput: (
      value: string,
      siblings: FormState,
    ) => boolean = (value: string, siblings: FormState) => {
      return !(
        ((value == "startDate" || value == "endDate") &&
          searchStore.temporalEventState.startDateOrEndDateFormattedValue != "") ||
        siblings.tempEventInput != undefined
      );
    };

    const rules = {
      startDateOrEndDateFormattedValue: {},
      startDateOrEndDateOption: { tempEventCheckForInput },
    };

    const v$ = useVuelidate(rules, searchStore.temporalEventState);

    const errorTempEventInput = computed(() => {
      const errors: Array<string> = [];
      if (
        searchStore.temporalEventState.startDateOrEndDateFormattedValue == "" ||
        startDateOrEndDate.value == undefined
      ) {
        errors.push("Eintrag wird benötigt");
      }
      return errors;
    });

    const errorTempEventStartEnd = computed(() => {
      const errors: Array<string> = [];
      if (
        !v$.value.startDateOrEndDateOption.$invalid &&
        searchStore.temporalEventState.startDateOrEndDateFormattedValue != undefined &&
          searchStore.temporalEventState.startDateOrEndDateFormattedValue != ""
      ) {
        errors.push("Wähle eine dieser Optionen aus");
      }
      return errors;
    });

    // Reset the search filter
    const canReset = computed(() => {
      return (
        searchStore.publicationDateFrom != "" ||
        searchStore.publicationDateTo != "" ||
        searchStore.accessStateOpen ||
        searchStore.accessStateRestricted ||
        searchStore.accessStateClosed ||
        searchStore.temporalEventState.startDateOrEndDateFormattedValue != "" ||
        searchStore.temporalEventState.startDateOrEndDateOption != "" ||
        searchStore.accessStateClosed ||
        searchStore.accessStateOpen ||
        searchStore.accessStateRestricted ||
        searchStore.formalRuleLicenceContract ||
        searchStore.formalRuleOpenContentLicence ||
        searchStore.formalRuleUserAgreement ||
        searchStore.temporalValidityFilterFuture ||
        searchStore.temporalValidityFilterPresent ||
        searchStore.temporalValidityFilterPast ||
        searchStore.temporalValidOnFormatted != "" ||
        searchStore.accessStateIdx.filter((element) => element).length > 0 ||
        searchStore.paketSigelIdIdx.filter((element) => element).length > 0 ||
        searchStore.zdbIdIdx.filter((element) => element).length > 0 ||
        searchStore.seriesIdx.filter((element) => element).length > 0 ||
        searchStore.templateNameIdx.filter((element) => element).length > 0 ||
        searchStore.publicationTypeIdx.filter((element) => element).length > 0 ||
        searchStore.licenceUrlIdx.filter((element) => element).length > 0 ||
        searchStore.noRightInformation ||
        searchStore.searchTerm ||
        searchStore.isLastSearchForTemplates ||
        searchStore.manualRight ||
        searchStore.accessStateOnDateState.dateValueFormatted ||
        searchStore.accessStateOnDateState.accessState
      );
    });

    const resetFilter: () => void = () => {
      searchStore.publicationDateFrom = "";
      searchStore.publicationDateTo = "";

      searchStore.accessStateOpen = false;
      searchStore.accessStateRestricted = false;
      searchStore.accessStateClosed = false;

      searchStore.accessStateClosed = false;
      searchStore.accessStateOpen = false;
      searchStore.accessStateRestricted = false;

      searchStore.formalRuleLicenceContract = false;
      searchStore.formalRuleOpenContentLicence = false;
      searchStore.formalRuleUserAgreement = false;

      searchStore.temporalValidityFilterFuture = false;
      searchStore.temporalValidityFilterPresent = false;
      searchStore.temporalValidityFilterPast = false;

      searchStore.temporalValidOnFormatted = "";
      searchStore.temporalEventState.startDateOrEndDateFormattedValue = "";
      searchStore.temporalEventState.startDateOrEndDateOption = "";

      searchStore.accessStateIdx = searchStore.accessStateIdx.map(() => false);
      searchStore.paketSigelIdIdx = searchStore.paketSigelIdIdx.map(
        () => false,
      );
      searchStore.publicationTypeIdx = searchStore.publicationTypeIdx.map(
        () => false,
      );
      searchStore.templateNameIdx = searchStore.templateNameIdx.map(
        () => false,
      );
      searchStore.zdbIdIdx = searchStore.zdbIdIdx.map(() => false);
      searchStore.seriesIdx = searchStore.seriesIdx.map(() => false);
      searchStore.licenceUrlIdx = searchStore.licenceUrlIdx.map(() => false);
      searchStore.noRightInformation = false;
      searchStore.manualRight = false;
      searchStore.accessStateOnDateState.dateValueFormatted = "";
      searchStore.accessStateOnDateState.accessState = "";
      searchStore.accessStateOnDateIdx = [] as Array<string>;
      emit("startEmptySearch");
    };

    const parseAccessState = (accessState: string, count: number) => {
      switch (accessState) {
        case "closed":
          return "Closed " + "(" + count + ")";
        case "open":
          return "Open " + "(" + count + ")";
        case "restricted":
          return "Restricted " + "(" + count + ")";
      }
    };
    const parsePublicationType = (pubType: string, count: number) => {
      return (
        metadata_utils.prettyPrintPublicationType(pubType) + " (" + count + ")"
      );
    };

    const ppPaketSigel = (paketSigel: string, count: number) => {
      return paketSigel + " (" + count + ")";
    };

    const ppLicenceUrl = (licenceUrl: string, count: number) => {
      if (licenceUrl == 'andere'){
        return "Andere (" + count + ")";
      } else {
        return licenceUrl + " (" + count + ")";
      }
    };

    const ppZDBId = (zdbId: string, count: number) => {
      return zdbId + " (" + count + ")";
    };

    /**
     * Bookmark settings.
     */
    const dialogStore = useDialogsStore();
    const activateBookmarkSaveDialog = () => {
      dialogStore.bookmarkSaveActivated = true;
    };

    /**
     * Search for AccessStateOnDate
     */
    const errorFetchBackendData = ref("");
    const getAccessStatesForDate = () => {
      api
          .searchQuery(
              "",
              0,
              1,
              0,
              true,
              undefined,
              undefined,
              undefined,
              undefined,
              undefined,
              undefined,
              undefined,
              searchStore.accessStateOnDateState.dateValueFormatted, // The interesting line
              undefined,
              undefined,
              undefined,
              undefined,
              undefined,
              undefined,
              undefined,
              undefined,
          ).then((response: ItemInformation) => {
            if (response.accessStateWithCount != undefined) {
              searchStore.accessStateOnDateReceived = response.accessStateWithCount;
            }
          })
          .catch((e) => {
            error.errorHandling(e, (errMsg: string) => {
              errorFetchBackendData.value = errMsg;
            });
          });
    }

    /**
     * Menu interactions.
     */
    const isStartEndDateMenuOpen = ref(false);
    const isAccessStateOnDateMenuOpen = ref(false);
    const accessStateDate = ref(undefined as Date | undefined);
    const startDateOrEndDate = ref(undefined as Date | undefined);

    const startDateOrEndDateEntered = () => {
      if (startDateOrEndDate.value != undefined) {
        searchStore.temporalEventState.startDateOrEndDateFormattedValue = date_utils.dateToIso8601(startDateOrEndDate.value);
      } else {
        searchStore.temporalEventState.startDateOrEndDateFormattedValue = "";
      }
    };

    watch(startDateOrEndDate, () => {
      isStartEndDateMenuOpen.value = false;
    });

    const accessStateDateEntered = () => {
      if (accessStateDate.value != undefined) {
        searchStore.accessStateOnDateState.dateValueFormatted = date_utils.dateToIso8601(accessStateDate.value);
      } else {
        searchStore.accessStateOnDateState.dateValueFormatted = "";
      }
    };

    watch(accessStateDate, () => {
      isAccessStateOnDateMenuOpen.value = false;
      getAccessStatesForDate();
      emitSearchStartAccessStateOn();
    });

    const singleSelectionAccessStateOnDate = () => {
      searchStore.accessStateOnDateIdx = [] as Array<string>;
    };

    const isValidOnMenuOpen = ref(false);
    const temporalValidOn = ref(undefined as Date | undefined);

    const temporalValidOnEntered = () => {
      if (temporalValidOn.value != undefined) {
        searchStore.temporalValidOnFormatted = date_utils.dateToIso8601(temporalValidOn.value);
      } else {
        searchStore.temporalValidOnFormatted = "";
      }
      emit("startSearch");
    };

    watch(temporalValidOn, () => {
      isValidOnMenuOpen.value = false;
    });

    const emitSearchStart = () => {
      emit("startSearch");
    };

    const emitSearchStartPublicationDate = (date: string) => {
      if (date.length == 4 || date.length == 0){
        emit("startSearch");
      }
    };

    const emitSearchStartAccessStateOn = () => {
      if (searchStore.accessStateOnDateState.dateValueFormatted != undefined &&
        searchStore.accessStateOnDateState.dateValueFormatted != "" &&
        searchStore.accessStateOnDateIdx.filter((e) => e != undefined).length == 1){
        emit("startSearch");
      }
    };

    return {
      accessStateDate,
      canReset,
      errorFetchBackendData,
      errorTempEventStartEnd,
      errorTempEventInput,
      temporalValidOn,
      isAccessStateOnDateMenuOpen,
      isStartEndDateMenuOpen,
      isValidOnMenuOpen,
      startDateOrEndDate,
      temporalEvent,
      tempEventMenu,
      tempValidOnMenu,
      searchStore,
      v$,
      accessStateDateEntered,
      activateBookmarkSaveDialog,
      emitSearchStart,
      emitSearchStartAccessStateOn,
      emitSearchStartPublicationDate,
      getAccessStatesForDate,
      parseAccessState,
      parsePublicationType,
      ppLicenceUrl,
      ppPaketSigel,
      ppZDBId,
      resetFilter,
      singleSelectionAccessStateOnDate,
      startDateOrEndDateEntered,
      temporalValidOnEntered,
    };
  },
});
</script>
<style scoped>
.scroll {
  overflow-y: scroll;
}
</style>
<template>
  <v-card height="100%" class="scroll">
    <v-row>
      <v-col cols="auto">
        <v-btn
          class="ml-8 mt-6"
          color="warning"
          :disabled="!canReset"
          @click="resetFilter"
        >
          Suche resetten</v-btn
        >
      </v-col>
    </v-row>
    <v-row>
      <v-col cols="auto">
        <v-btn
          class="ml-8"
          color="blue darken-1"
          @click="activateBookmarkSaveDialog"
        >
          Suche speichern
        </v-btn>
      </v-col>
    </v-row>
    <v-row>
      <v-col>
        <v-card-title>Publikationsfilter</v-card-title>
      </v-col>
    </v-row>
    <v-container fluid>
      <v-row>
        <v-col cols="12">
          <v-list>
            <v-list-group sub-group>
              <template v-slot:activator="{ props }">
                <v-list-item
                  v-bind="props"
                  title="Publikationsjahr"
                ></v-list-item>
              </template>
              <v-list-item>
                <v-row>
                  <v-col cols="6">
                    <v-text-field
                      label="Von"
                      v-model="searchStore.publicationDateFrom"
                      @update:modelValue="emitSearchStartPublicationDate(searchStore.publicationDateFrom)"
                    ></v-text-field>
                  </v-col>
                  <v-col cols="6">
                    <v-text-field
                      label="Bis"
                      v-model="searchStore.publicationDateTo"
                      @update:modelValue="emitSearchStartPublicationDate(searchStore.publicationDateTo)"
                    ></v-text-field>
                  </v-col>
                </v-row>
              </v-list-item>
            </v-list-group>
            <v-list-group sub-group>
              <template v-slot:activator="{ props }">
                <v-list-item
                  v-bind="props"
                  title="Publikationstyp"
                ></v-list-item>
              </template>
              <h6></h6>
              <v-list>
                <v-list-item
                    v-for="(item, i) in searchStore.publicationTypeReceived"
                    :key="i"
                    :value="item"
                    color="primary"
                    rounded="shaped"
                >
                  <v-checkbox
                      :label="parsePublicationType(item.publicationType, item.count)"
                      hide-details
                      class="pl-9 ml-4"
                      v-model="searchStore.publicationTypeIdx[i]"
                      @update:modelValue="emitSearchStart"
                  ></v-checkbox>
                  <v-divider
                      :thickness="1"
                      class="border-opacity-100"
                      color="grey-lighten-1"
                  ></v-divider>
                </v-list-item>
              </v-list>
            </v-list-group>
            <v-list-group sub-group>
              <template v-slot:activator="{ props }">
                <v-list-item v-bind="props" title="Paketsigel"></v-list-item>
              </template>
              <h6></h6>
              <v-list>
                <v-list-item
                    v-for="(item, i) in searchStore.paketSigelIdReceived"
                    :key="i"
                    :value="item"
                    color="primary"
                    rounded="shaped"
                >
                  <v-checkbox
                      :label="ppPaketSigel(item.paketSigel, item.count)"
                      hide-details
                      class="pl-9 ml-4"
                      v-model="searchStore.paketSigelIdIdx[i]"
                      @update:modelValue="emitSearchStart"
                  ></v-checkbox>
                  <v-divider
                      :thickness="1"
                      class="border-opacity-100"
                      color="grey-lighten-1"
                  ></v-divider>
                </v-list-item>
              </v-list>
            </v-list-group>
            <v-list-group sub-group>
              <template v-slot:activator="{ props }">
                <v-list-item v-bind="props" title="Serie"></v-list-item>
              </template>
              <h6></h6>
              <v-list>
                <v-list-item
                    v-for="(item, i) in searchStore.seriesReceived"
                    :key="i"
                    :value="item"
                    color="primary"
                    rounded="shaped"
                >
                  <v-checkbox
                      :label="ppZDBId(item.series, item.count)"
                      hide-details
                      class="pl-9 ml-4"
                      v-model="searchStore.seriesIdx[i]"
                      @update:modelValue="emitSearchStart"
                  ></v-checkbox>
                  <v-divider
                      :thickness="1"
                      class="border-opacity-100"
                      color="grey-lighten-1"
                  ></v-divider>
                </v-list-item>
              </v-list>
            </v-list-group>
            <v-list-group sub-group>
              <template v-slot:activator="{ props }">
                <v-list-item v-bind="props" title="ZDB-IDs"></v-list-item>
              </template>
              <h6></h6>
              <v-list>
                <v-list-item
                    v-for="(item, i) in searchStore.zdbIdReceived"
                    :key="i"
                    :value="item"
                    color="primary"
                    rounded="shaped"
                >
                  <v-checkbox
                      :label="ppZDBId(item.zdbId, item.count)"
                      hide-details
                      class="pl-9 ml-4"
                      v-model="searchStore.zdbIdIdx[i]"
                      @update:modelValue="emitSearchStart"
                  ></v-checkbox>
                  <v-divider
                      :thickness="1"
                      class="border-opacity-100"
                      color="grey-lighten-1"
                  ></v-divider>
                </v-list-item>
              </v-list>
            </v-list-group>
            <v-list-group sub-group>
              <template v-slot:activator="{ props }">
                <v-list-item v-bind="props" title="CC-Lizenz"></v-list-item>
              </template>
              <h6></h6>
              <v-list>
                <v-list-item
                    v-for="(item, i) in searchStore.licenceUrlReceived"
                    :key="i"
                    :value="item"
                    color="primary"
                    rounded="shaped"
                >
                  <v-checkbox
                      :label="ppLicenceUrl(item.licenceUrl, item.count)"
                      hide-details
                      class="pl-9 ml-4"
                      v-model="searchStore.licenceUrlIdx[i]"
                      @update:modelValue="emitSearchStart"
                  ></v-checkbox>
                  <v-divider
                      :thickness="1"
                      class="border-opacity-100"
                      color="grey-lighten-1"
                  ></v-divider>
                </v-list-item>
              </v-list>
            </v-list-group>
          </v-list>
        </v-col>
      </v-row>
    </v-container>
    <v-card-title>Rechteinformationsfilter</v-card-title>
    <v-container fluid>
      <v-row>
        <v-col cols="12">
          <v-list>
            <v-list-group sub-group>
              <template v-slot:activator="{ props }">
                <v-list-item v-bind="props" title="Access-Status"></v-list-item>
              </template>
              <h6></h6>
              <v-list>
                <v-list-item
                    v-for="(item, i) in searchStore.accessStateReceived"
                    :key="i"
                    :value="item"
                    color="primary"
                    rounded="shaped"
                >
                  <v-checkbox
                      :label="parseAccessState(item.accessState, item.count)"
                      hide-details
                      class="pl-9 ml-4"
                      v-model="searchStore.accessStateIdx[i]"
                      @update:modelValue="emitSearchStart"
                  ></v-checkbox>
                  <v-divider
                      :thickness="1"
                      class="border-opacity-100"
                      color="grey-lighten-1"
                  ></v-divider>
                </v-list-item>
              </v-list>
            </v-list-group>
            <v-list-group sub-group>
              <template v-slot:activator="{ props }">
                <v-list-item
                    v-bind="props"
                    title="Access-Status am"
                >
                </v-list-item>
              </template>
              <v-menu
                  :location="'bottom'"
                  :close-on-content-click="false"
                  v-model="isAccessStateOnDateMenuOpen"
              >
                <template v-slot:activator="{ props }">
                  <v-text-field
                      v-model="searchStore.accessStateOnDateState.dateValueFormatted"
                      prepend-icon="mdi-calendar"
                      v-bind="props"
                      readonly
                      required
                      clearable
                      @update:modelValue="getAccessStatesForDate"
                      :error-messages="errorFetchBackendData"
                  ></v-text-field>
                </template>
                <v-date-picker
                    v-model="accessStateDate"
                    color="primary"
                    @update:modelValue="accessStateDateEntered"
                ><template v-slot:header></template>
                </v-date-picker>
              </v-menu>
              <h6></h6>
              <v-list>
                <v-list-item
                    v-for="(item, i) in searchStore.accessStateOnDateReceived"
                    :key="i"
                    :value="item"
                    color="primary"
                    rounded="shaped"
                >
                  <v-checkbox
                      :label="parseAccessState(item.accessState, item.count)"
                      hide-details
                      class="pl-9 ml-4"
                      v-model="searchStore.accessStateOnDateIdx"
                      :value="item.accessState"
                      @click="singleSelectionAccessStateOnDate"
                      @update:modelValue="emitSearchStartAccessStateOn"
                  ></v-checkbox>
                  <v-divider
                      :thickness="1"
                      class="border-opacity-100"
                      color="grey-lighten-1"
                  ></v-divider>
                </v-list-item>
              </v-list>
            </v-list-group>
            <v-list-group sub-group>
              <template v-slot:activator="{ props }">
                <v-list-item
                  v-bind="props"
                  title="Zeitliche Gültigkeit am"
                ></v-list-item>
              </template>
              <v-menu
                :close-on-content-click="false"
                :location="'bottom'"
                v-model="isValidOnMenuOpen"
              >
                <template v-slot:activator="{ props }">
                  <v-text-field
                    v-model="searchStore.temporalValidOnFormatted"
                    prepend-icon="mdi-calendar"
                    v-bind="props"
                    readonly
                    clearable
                    @update:modelValue="emitSearchStart"
                  ></v-text-field>
                </template>
                <v-date-picker
                  v-model="temporalValidOn"
                  color="primary"
                  @update:modelValue="temporalValidOnEntered"
                  ><template v-slot:header></template>
                </v-date-picker>
              </v-menu>
            </v-list-group>
            <v-list-group sub-group>
              <template v-slot:activator="{ props }">
                <v-list-item
                  v-bind="props"
                  title="Zeitliche Gültigkeit Ereignis"
                >
                </v-list-item>
              </template>
              <v-menu
                :location="'bottom'"
                :close-on-content-click="false"
                v-model="isStartEndDateMenuOpen"
              >
                <template v-slot:activator="{ props }">
                  <v-text-field
                    v-model="searchStore.temporalEventState.startDateOrEndDateFormattedValue"
                    prepend-icon="mdi-calendar"
                    v-bind="props"
                    readonly
                    required
                    clearable
                    @change="v$.startDateOrEndDateFormattedValue.$touch()"
                    @blur="v$.startDateOrEndDateFormattedValue.$touch()"
                    :error-messages="errorTempEventInput"
                    @update:modelValue="emitSearchStart"
                  ></v-text-field>
                </template>
                <v-date-picker
                  v-model="startDateOrEndDate"
                  color="primary"
                  @update:modelValue="startDateOrEndDateEntered"
                  ><template v-slot:header></template>
                  <v-spacer></v-spacer>
                </v-date-picker>
              </v-menu>
              <v-item-group v-model="temporalEvent">
                <v-item>
                  <v-checkbox
                    label="Startdatum"
                    class="pl-9 ml-4"
                    hide-details
                    v-model="
                      searchStore.temporalEventState.startDateOrEndDateOption
                    "
                    value="startDate"
                    :error-messages="errorTempEventStartEnd"
                    @update:modelValue="emitSearchStart"
                  ></v-checkbox>
                </v-item>
                <v-divider
                    :thickness="1"
                    class="border-opacity-100"
                    color="grey-lighten-1"
                ></v-divider>
                <v-item>
                  <v-checkbox
                    label="Enddatum"
                    class="pl-9 ml-4"
                    v-model="
                      searchStore.temporalEventState.startDateOrEndDateOption
                    "
                    :error-messages="errorTempEventStartEnd"
                    value="endDate"
                    @update:modelValue="emitSearchStart"
                  ></v-checkbox>
                </v-item>
              </v-item-group>
            </v-list-group>
            <v-list-group sub-group>
              <template v-slot:activator="{ props }">
                <v-list-item
                  v-bind="props"
                  title="Zeitliche Gültigkeit"
                ></v-list-item>
              </template>
              <v-checkbox
                label="Vergangenheit"
                hide-details
                class="pl-9 ml-4"
                v-model="searchStore.temporalValidityFilterPast"
                @update:modelValue="emitSearchStart"
              ></v-checkbox>
              <v-divider
                  :thickness="1"
                  class="border-opacity-100"
                  color="grey-lighten-1"
              ></v-divider>
              <v-checkbox
                label="Aktuell"
                hide-details
                class="pl-9 ml-4"
                v-model="searchStore.temporalValidityFilterPresent"
                @update:modelValue="emitSearchStart"
              ></v-checkbox>
              <v-divider
                  :thickness="1"
                  class="border-opacity-100"
                  color="grey-lighten-1"
              ></v-divider>
              <v-checkbox
                label="Zukunft"
                hide-details
                class="pl-9 ml-4"
                v-model="searchStore.temporalValidityFilterFuture"
                @update:modelValue="emitSearchStart"
              ></v-checkbox>
            </v-list-group>
            <v-list-group sub-group>
              <template v-slot:activator="{ props }">
                <v-list-item
                  v-bind="props"
                  title="Formale Regelung"
                ></v-list-item>
              </template>
              <h6></h6>
              <v-checkbox
                v-if="searchStore.hasLicenceContract"
                label="Lizenzvertrag"
                hide-details
                class="pl-9 ml-4"
                v-model="searchStore.formalRuleLicenceContract"
                @update:modelValue="emitSearchStart"
              ></v-checkbox>
              <v-divider
                  :thickness="1"
                  class="border-opacity-100"
                  color="grey-lighten-1"
              ></v-divider>
              <v-checkbox
                  v-if="searchStore.hasOpenContentLicence"
                  label="Open-Content-Licence"
                  hide-details
                  class="pl-9 ml-4"
                  v-model="searchStore.formalRuleOpenContentLicence"
                  @update:modelValue="emitSearchStart"
              ></v-checkbox>
              <v-divider
                  :thickness="1"
                  class="border-opacity-100"
                  color="grey-lighten-1"
              ></v-divider>
              <v-checkbox
                v-if="searchStore.hasZbwUserAgreement"
                label="ZBW-Nutzungsvereinbarung"
                hide-details
                class="pl-9 ml-4"
                v-model="searchStore.formalRuleUserAgreement"
                @update:modelValue="emitSearchStart"
              ></v-checkbox>
            </v-list-group>
            <v-list-group no-action sub-group eager>
              <template v-slot:activator="{ props }">
                <v-list-item v-bind="props" title="Allgemein"></v-list-item>
              </template>
              <h6></h6>
              <v-checkbox
                label="Keine Rechteeinträge"
                hide-details
                class="pl-9 ml-4"
                v-model="searchStore.noRightInformation"
                @update:modelValue="emitSearchStart"
              ></v-checkbox>
              <v-divider
                  :thickness="1"
                  class="border-opacity-100"
                  color="grey-lighten-1"
              ></v-divider>
              <v-checkbox
                  label="Manuell erstellte Rechteeinträge"
                  hide-details
                  class="pl-9 ml-4"
                  v-model="searchStore.manualRight"
                  @update:modelValue="emitSearchStart"
              ></v-checkbox>
            </v-list-group>
            <v-list-group sub-group>
              <template v-slot:activator="{ props }">
                <v-list-item
                  v-bind="props"
                  title="Template-Namen"
                ></v-list-item>
              </template>
              <h6></h6>
              <v-list>
                <v-list-item
                    v-for="(item, i) in searchStore.templateNameReceived"
                    :key="i"
                    :value="item"
                    color="primary"
                    rounded="shaped"
                >
                  <v-checkbox
                      :label="ppZDBId(item.templateName, item.count)"
                      hide-details
                      class="pl-9 ml-4"
                      v-model="searchStore.templateNameIdx[i]"
                      @update:modelValue="emitSearchStart"
                  ></v-checkbox>
                  <v-divider
                      :thickness="1"
                      class="border-opacity-100"
                      color="grey-lighten-1"
                  ></v-divider>
                </v-list-item>
              </v-list>
            </v-list-group>
          </v-list>
        </v-col>
      </v-row>
    </v-container>
  </v-card>
</template>
