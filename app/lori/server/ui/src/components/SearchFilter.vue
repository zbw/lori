<script lang="ts">
import { computed, defineComponent, ref, watch } from "vue";
import { useSearchStore } from "@/stores/search";
import { useVuelidate } from "@vuelidate/core";
import { useDialogsStore } from "@/stores/dialogs";
import date_utils from "@/utils/date_utils";
import metadata_utils from "@/utils/metadata_utils";
import {useUserStore} from "@/stores/user";

export default defineComponent({
  emits: ["startEmptySearch", "startSearch", "getAccessStatesOnDate"],
  setup(props, { emit }) {
    const searchStore = useSearchStore();
    const userStore = useUserStore();
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
        searchStore.publicationYearFrom != "" ||
        searchStore.publicationYearTo != "" ||
        searchStore.accessStateOpen ||
        searchStore.accessStateRestricted ||
        searchStore.accessStateClosed ||
        searchStore.temporalEventState.startDateOrEndDateFormattedValue != "" ||
        searchStore.temporalEventState.startDateOrEndDateOption != "" ||
        searchStore.accessStateClosed ||
        searchStore.accessStateOpen ||
        searchStore.accessStateRestricted ||
        searchStore.formalRuleLicenceContract ||
        searchStore.formalRuleCCNoRestriction ||
        searchStore.formalRuleNoLegalRisk ||
        searchStore.formalRuleUserAgreement ||
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
      searchStore.publicationYearFrom = "";
      searchStore.publicationYearTo = "";

      searchStore.accessStateOpen = false;
      searchStore.accessStateRestricted = false;
      searchStore.accessStateClosed = false;

      searchStore.accessStateClosed = false;
      searchStore.accessStateOpen = false;
      searchStore.accessStateRestricted = false;

      searchStore.formalRuleLicenceContract = false;
      searchStore.formalRuleCCNoRestriction = false;
      searchStore.formalRuleNoLegalRisk = false;
      searchStore.formalRuleUserAgreement = false;

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
      switch (accessState.toLowerCase()) {
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

    const ppLicenceContracts = (count: number) => {
      return "Lizenzvertrag " + "(" + count + ")";
    };

    const ppCCLicenceNoRestriction = (count: number) => {
      return "CC-Lizenz ohne Einschränkung " + "(" + count + ")";
    };

    const ppZBWUserAgreements = (count: number) => {
      return "ZBW-Nutzungsvereinbarung " + "(" + count + ")";
    };

    const ppNoLegalRisk = (count: number) => {
      return "Anwendbarkeit Urheberrechtschranke ohne vertragsrechtliches Risiko " + "(" + count + ")";
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
    const emitGetAccessStateOnDateSearch = () => {
      emit("getAccessStatesOnDate");
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
      emitGetAccessStateOnDateSearch();
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
      resetPerPageValues();
      emit("startSearch");
    };

    const resetPerPageValues = () => {
      seriesPerPage.value = 20;
      zdbPerPage.value = 20;
      licencePerPage.value = 20;
      sigelPerPage.value = 20;
    };

    const emitSearchStartPublicationYear = (date: string) => {
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

    /**
     * Lazy loading
     */
    // Series
    const seriesToShow = computed(() => {
      if (seriesPerPage.value == 20) {
        rerenderId.value += 1;
      }
      return searchStore.seriesReceived.slice(0, seriesPerPage.value)
    });
    const isSeriesGroupOpen = ref(false); // Track if the group is open
    const isSeriesLoading = ref(false); // Prevent multiple loads
    const seriesPerPage = ref(20); // Number of items to load per scroll
    const rerenderId = ref(0);
    const scrollWrapperSeries = ref<HTMLElement | null>(null); // Scroll wrapper ref
    const handleScrollSeries = (event: Event) => {
      if (scrollWrapperSeries.value) {
        const { scrollHeight, scrollTop, clientHeight } = scrollWrapperSeries.value;
        // If the user has scrolled to the bottom
        let diff = Math.abs(clientHeight - (scrollHeight - scrollTop));
        if (diff < 3) {
          loadMoreSeries();
        }
      }
    };

    const loadMoreSeries = () => {
      isSeriesLoading.value = true;
      // Simulate loading more items
      setTimeout(() => {
        seriesPerPage.value += 20; // Load more items as the user scrolls
        isSeriesLoading.value = false;
      }, 500); // Simulate API delay (adjust as needed)
    };

    // PaketSigel
    const sigelToShow = computed(() => {
      return searchStore.paketSigelIdReceived.slice(0, sigelPerPage.value)
    });
    const isSigelGroupOpen = ref(false); // Track if the group is open
    const isSigelLoading = ref(false); // Prevent multiple loads
    const sigelPerPage = ref(20); // Number of items to load per scroll
    const scrollWrapperSigel = ref<HTMLElement | null>(null); // Scroll wrapper ref
    const handleScrollSigel = (event: Event) => {
      if (scrollWrapperSigel.value) {
        const { scrollHeight, scrollTop, clientHeight } = scrollWrapperSigel.value;
        // If the user has scrolled to the bottom
        let diff = Math.abs(clientHeight - (scrollHeight - scrollTop));
        if (diff < 3) {
          loadMoreSigel();
        }
      }
    };

    const loadMoreSigel = () => {
      isSigelLoading.value = true;
      // Simulate loading more items
      setTimeout(() => {
        sigelPerPage.value += 20; // Load more items as the user scrolls
        isSigelLoading.value = false;
      }, 500); // Simulate API delay (adjust as needed)
    };

    // ZDB-Ids
    const zdbToShow = computed(() => {
      return searchStore.zdbIdReceived.slice(0, zdbPerPage.value)
    });
    const isZdbGroupOpen = ref(false); // Track if the group is open
    const isZdbLoading = ref(false); // Prevent multiple loads
    const zdbPerPage = ref(20); // Number of items to load per scroll
    const scrollWrapperZdb = ref<HTMLElement | null>(null); // Scroll wrapper ref
    const handleScrollZdb = (event: Event) => {
      if (scrollWrapperZdb.value) {
        const { scrollHeight, scrollTop, clientHeight } = scrollWrapperZdb.value;
        // If the user has scrolled to the bottom
        let diff = Math.abs(clientHeight - (scrollHeight - scrollTop));
        if (diff < 3) {
          loadMoreZdb();
        }
      }
    };

    const loadMoreZdb = () => {
      isZdbLoading.value = true;
      // Simulate loading more items
      setTimeout(() => {
        zdbPerPage.value += 20; // Load more items as the user scrolls
        isZdbLoading.value = false;
      }, 500); // Simulate API delay (adjust as needed)
    };

    // Licence-URL
    const licenceToShow = computed(() => {
      return searchStore.licenceUrlReceived.slice(0, licencePerPage.value)
    });
    const isLicenceGroupOpen = ref(false); // Track if the group is open
    const isLicenceLoading = ref(false); // Prevent multiple loads
    const licencePerPage = ref(20); // Number of items to load per scroll
    const scrollWrapperLicence = ref<HTMLElement | null>(null); // Scroll wrapper ref
    const handleScrollLicence = (event: Event) => {
      if (scrollWrapperLicence.value) {
        const { scrollHeight, scrollTop, clientHeight } = scrollWrapperLicence.value;
        // If the user has scrolled to the bottom
        let diff = Math.abs(clientHeight - (scrollHeight - scrollTop));
        if (diff < 3) {
          loadMoreLicence();
        }
      }
    };

    const loadMoreLicence = () => {
      isLicenceLoading.value = true;
      // Simulate loading more items
      setTimeout(() => {
        licencePerPage.value += 20; // Load more items as the user scrolls
        isLicenceLoading.value = false;
      }, 500); // Simulate API delay (adjust as needed)
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
      isLicenceGroupOpen,
      isSeriesGroupOpen,
      isSigelGroupOpen,
      isZdbGroupOpen,
      licenceToShow,
      seriesToShow,
      sigelToShow,
      zdbToShow,
      isLicenceLoading,
      isSeriesLoading,
      isSigelLoading,
      isZdbLoading,
      licencePerPage,
      rerenderId,
      seriesPerPage,
      sigelPerPage,
      zdbPerPage,
      scrollWrapperLicence,
      scrollWrapperSeries,
      scrollWrapperSigel,
      scrollWrapperZdb,
      userStore,
      accessStateDateEntered,
      activateBookmarkSaveDialog,
      emitGetAccessStateOnDateSearch,
      emitSearchStart,
      emitSearchStartAccessStateOn,
      emitSearchStartPublicationYear,
      handleScrollLicence,
      handleScrollSeries,
      handleScrollSigel,
      handleScrollZdb,
      loadMoreLicence,
      loadMoreSigel,
      loadMoreSeries,
      loadMoreZdb,
      parseAccessState,
      parsePublicationType,
      ppCCLicenceNoRestriction,
      ppLicenceContracts,
      ppLicenceUrl,
      ppNoLegalRisk,
      ppPaketSigel,
      ppZBWUserAgreements,
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
          :disabled="!userStore.isLoggedIn"
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
                      v-model="searchStore.publicationYearFrom"
                      @update:modelValue="emitSearchStartPublicationYear(searchStore.publicationYearFrom)"
                    ></v-text-field>
                  </v-col>
                  <v-col cols="6">
                    <v-text-field
                      label="Bis"
                      v-model="searchStore.publicationYearTo"
                      @update:modelValue="emitSearchStartPublicationYear(searchStore.publicationYearTo)"
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
            <v-list-group sub-group v-model="isSigelGroupOpen">
              <template v-slot:activator="{ props }">
                <v-list-item v-bind="props" title="Paketsigel"></v-list-item>
              </template>
              <h6></h6>
              <v-expand-transition>
                <div ref="scrollWrapperSigel" @scroll="handleScrollSigel" style="max-height: 400px; overflow-y: auto;">
                  <v-virtual-scroll
                      :items="sigelToShow"
                      item-height="64"
                      v-slot:default="{ item, index }"
                      :items-per-page="sigelPerPage"
                  >
                    <v-list-item
                        :key="item.paketSigel + rerenderId"
                        :value="item"
                        color="primary"
                        rounded="shaped"
                    >
                      <v-checkbox
                          :label="ppPaketSigel(item.paketSigel, item.count)"
                          hide-details
                          class="pl-9 ml-4"
                          v-model="searchStore.paketSigelIdIdx[index]"
                          @update:modelValue="emitSearchStart"
                      ></v-checkbox>
                      <v-divider
                          :thickness="1"
                          class="border-opacity-100"
                          color="grey-lighten-1"
                      ></v-divider>
                    </v-list-item>
                  </v-virtual-scroll>
                </div>
              </v-expand-transition>
            </v-list-group>
            <v-list-group sub-group v-model="isSeriesGroupOpen">
              <template v-slot:activator="{ props }">
                <v-list-item v-bind="props" title="Serie"></v-list-item>
              </template>
              <h6></h6>
              <v-expand-transition>
                <div ref="scrollWrapperSeries" @scroll="handleScrollSeries" style="max-height: 400px; overflow-y: auto;">
                  <v-virtual-scroll
                      :items="seriesToShow"
                      item-height="64"
                      v-slot:default="{ item, index }"
                      :items-per-page="seriesPerPage"
                  >
                    <v-list-item
                        :key="item.series + rerenderId"
                        :value="item"
                        color="primary"
                        rounded="shaped"
                    >
                      <v-checkbox
                          :label="ppZDBId(item.series, item.count)"
                          hide-details
                          class="pl-9 ml-4"
                          v-model="searchStore.seriesIdx[index]"
                          @update:modelValue="emitSearchStart"
                      ></v-checkbox>
                      <v-divider
                          :thickness="1"
                          class="border-opacity-100"
                          color="grey-lighten-1"
                      ></v-divider>
                    </v-list-item>
                  </v-virtual-scroll>
                </div>
              </v-expand-transition>
            </v-list-group>
            <v-list-group sub-group>
              <template v-slot:activator="{ props }">
                <v-list-item v-bind="props" title="ZDB-IDs"></v-list-item>
              </template>
              <h6></h6>
              <v-expand-transition>
                <div ref="scrollWrapperZdb" @scroll="handleScrollZdb" style="max-height: 400px; overflow-y: auto;">
                  <v-virtual-scroll
                      :items="zdbToShow"
                      item-height="64"
                      v-slot:default="{ item, index }"
                      :items-per-page="zdbPerPage"
                  >
                    <v-list-item
                        :key="item.zdbId + rerenderId"
                        :value="item"
                        color="primary"
                        rounded="shaped"
                    >
                      <v-checkbox
                          :label="ppZDBId(item.zdbId, item.count)"
                          hide-details
                          class="pl-9 ml-4"
                          v-model="searchStore.zdbIdIdx[index]"
                          @update:modelValue="emitSearchStart"
                      ></v-checkbox>
                      <v-divider
                          :thickness="1"
                          class="border-opacity-100"
                          color="grey-lighten-1"
                      ></v-divider>
                    </v-list-item>
                  </v-virtual-scroll>
                </div>
              </v-expand-transition>
            </v-list-group>
            <v-list-group sub-group>
              <template v-slot:activator="{ props }">
                <v-list-item v-bind="props" title="CC-Lizenz"></v-list-item>
              </template>
              <h6></h6>
              <v-expand-transition>
                <div ref="scrollWrapperLicence" @scroll="handleScrollLicence" style="max-height: 400px; overflow-y: auto;">
                  <v-virtual-scroll
                      :items="licenceToShow"
                      item-height="64"
                      v-slot:default="{ item, index }"
                      :items-per-page="licencePerPage"
                  >
                    <v-list-item
                        :key="item.licenceUrl + rerenderId"
                        :value="item"
                        color="primary"
                        rounded="shaped"
                    >
                      <v-checkbox
                          :label="ppLicenceUrl(item.licenceUrl, item.count)"
                          hide-details
                          class="pl-9 ml-4"
                          v-model="searchStore.licenceUrlIdx[index]"
                          @update:modelValue="emitSearchStart"
                      ></v-checkbox>
                      <v-divider
                          :thickness="1"
                          class="border-opacity-100"
                          color="grey-lighten-1"
                      ></v-divider>
                    </v-list-item>
                  </v-virtual-scroll>
                </div>
              </v-expand-transition>
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
                      @update:modelValue="emitGetAccessStateOnDateSearch"
                      @click:clear="emitSearchStart"
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
                  title="Formale Regelung"
                ></v-list-item>
              </template>
              <h6></h6>
              <v-checkbox
                v-if="searchStore.licenceContracts > 0"
                :label=ppLicenceContracts(searchStore.licenceContracts)
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
                  v-if="searchStore.ccLicenceNoRestrictions > 0"
                  :label=ppCCLicenceNoRestriction(searchStore.ccLicenceNoRestrictions)
                  hide-details
                  class="pl-9 ml-4"
                  v-model="searchStore.formalRuleCCNoRestriction"
                  @update:modelValue="emitSearchStart"
              ></v-checkbox>
              <v-divider
                  :thickness="1"
                  class="border-opacity-100"
                  color="grey-lighten-1"
              ></v-divider>
              <v-checkbox
                v-if="searchStore.zbwUserAgreements > 0"
                :label=ppZBWUserAgreements(searchStore.zbwUserAgreements)
                hide-details
                class="pl-9 ml-4"
                v-model="searchStore.formalRuleUserAgreement"
                @update:modelValue="emitSearchStart"
              ></v-checkbox>
              <v-divider
                  :thickness="1"
                  class="border-opacity-100"
                  color="grey-lighten-1"
              ></v-divider>
              <v-checkbox
                  v-if="searchStore.noLegalRisks > 0"
                  :label=ppNoLegalRisk(searchStore.noLegalRisks)
                  hide-details
                  class="pl-9 ml-4"
                  v-model="searchStore.formalRuleNoLegalRisk"
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
