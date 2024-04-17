<script lang="ts">
import { computed, defineComponent, ref, watch } from "vue";
import { useSearchStore } from "@/stores/search";
import { useVuelidate } from "@vuelidate/core";
import { useDialogsStore } from "@/stores/dialogs";
import date_utils from "@/utils/date_utils";

export default defineComponent({
  emits: ["startEmptySearch"],
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
          startEndDateFormatted.value != "") ||
        siblings.tempEventInput != undefined
      );
    };

    const rules = {
      startDateOrEndDateValue: {},
      startDateOrEndDateOption: { tempEventCheckForInput },
    };

    const v$ = useVuelidate(rules, searchStore.temporalEventState);

    const errorTempEventInput = computed(() => {
      const errors: Array<string> = [];
      if (
        searchStore.temporalEventState.startDateOrEndDateValue == undefined ||
        startEndDateFormatted.value == ""
      ) {
        errors.push("Eintrag wird benötigt");
      }
      return errors;
    });

    const errorTempEventStartEnd = computed(() => {
      const errors: Array<string> = [];
      if (
        !v$.value.startDateOrEndDateOption.$invalid &&
        searchStore.temporalEventState.startDateOrEndDateValue != undefined
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
        searchStore.temporalEventState.startDateOrEndDateValue != undefined ||
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
        searchStore.temporalValidOn != undefined ||
        searchStore.accessStateIdx.filter((element) => element).length > 0 ||
        searchStore.paketSigelIdIdx.filter((element) => element).length > 0 ||
        searchStore.zdbIdIdx.filter((element) => element).length > 0 ||
        searchStore.templateNameIdx.filter((element) => element).length > 0 ||
        searchStore.publicationTypeIdx.filter((element) => element).length >
          0 ||
        searchStore.noRightInformation ||
        searchStore.searchTerm ||
        searchStore.isLastSearchForTemplates
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

      searchStore.temporalValidOn = undefined;
      searchStore.temporalEventState.startDateOrEndDateValue = undefined;
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
      searchStore.noRightInformation = false;
      emit("startEmptySearch");
    };

    const parseAccessState = (accessState: string, count: number) => {
      switch (accessState) {
        case "closed":
          return "Closed Access " + "(" + count + ")";
        case "open":
          return "Open Access " + "(" + count + ")";
        case "restricted":
          return "Restricted Access " + "(" + count + ")";
      }
    };
    const parsePublicationType = (pubType: string, count: number) => {
      switch (pubType) {
        case "article":
          return "Aufsatz/Article " + "(" + count + ")";
        case "book":
          return "Buch/Book " + "(" + count + ")";
        case "bookPart":
          return "Buchaufsatz/Book Part " + "(" + count + ")";
        /**
         * IMPORTANT NOTE: Openapis conversion of enums between frontend and backend
         * has issues with multiple word entries. The entries aren't always
         * encoded as the interface suggests, for instance 'periodicalPart' is
         * sometimes encoded as 'periodical_part'. That's the reason why all
         * these conversions contain both variants.
         */
        case "conference_paper":
          return "Konferenzschrift/\n Conference Paper " + "(" + count + ")";
        case "conferencePaper":
          return "Konferenzschrift/\n Conference Paper " + "(" + count + ")";
        case "periodical_part":
          return "Zeitschriftenband/\n Periodical Part " + "(" + count + ")";
        case "periodicalPart":
          return "Zeitschriftenband/\n Periodical Part " + "(" + count + ")";
        case "proceedings":
          return "Konferenzband/\n Proceeding " + "(" + count + ")";
        case "research_report":
          return "Forschungsbericht/\n Research Report " + "(" + count + ")";
        case "researchReport":
          return "Forschungsbericht/\n Research Report " + "(" + count + ")";
        case "thesis":
          return "Thesis " + "(" + count + ")";
        case "working_paper":
          return "Working Paper " + "(" + count + ")";
        case "workingPaper":
          return "Working Paper " + "(" + count + ")";
        default:
          return "Unknown pub type:" + pubType;
      }
    };

    const ppPaketSigel = (paketSigel: string, count: number) => {
      return paketSigel + " (" + count + ")";
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
     * Menu interactions.
     */
    const isStartEndDateMenuOpen = ref(false);

    const startEndDateFormatted = computed(() => {
      if (searchStore.temporalEventState.startDateOrEndDateValue != undefined) {
        return date_utils.dateToIso8601(
          searchStore.temporalEventState.startDateOrEndDateValue,
        );
      } else {
        return "";
      }
    });

    watch(startEndDateFormatted, () => {
      isStartEndDateMenuOpen.value = false;
    });

    const isValidOnMenuOpen = ref(false);
    const temporalValidOnFormatted = computed(() => {
      if (searchStore.temporalValidOn != undefined) {
        return date_utils.dateToIso8601(searchStore.temporalValidOn);
      } else {
        return "";
      }
    });

    watch(temporalValidOnFormatted, () => {
      isValidOnMenuOpen.value = false;
    });

    return {
      canReset,
      errorTempEventStartEnd,
      errorTempEventInput,
      temporalValidOnFormatted,
      isStartEndDateMenuOpen,
      isValidOnMenuOpen,
      startEndDateFormatted,
      temporalEvent,
      tempEventMenu,
      tempValidOnMenu,
      searchStore,
      v$,
      activateBookmarkSaveDialog,
      parseAccessState,
      parsePublicationType,
      ppPaketSigel,
      ppZDBId,
      resetFilter,
    };
  },
});
</script>
<template>
  <v-card height="100%">
    <v-row>
      <v-col cols="auto">
        <v-btn
          color="warning"
          :disabled="!canReset"
          @click="resetFilter"
          size="large"
        >
          Suche resetten</v-btn
        >
      </v-col>
    </v-row>
    <v-row>
      <v-col cols="auto">
        <v-btn
          color="blue darken-1"
          @click="activateBookmarkSaveDialog"
          size="large"
        >
          Suchfilter speichern
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
                    ></v-text-field>
                  </v-col>
                  <v-col cols="6">
                    <v-text-field
                      label="Bis"
                      v-model="searchStore.publicationDateTo"
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
              <v-checkbox
                v-for="(item, i) in searchStore.publicationTypeReceived"
                :key="i"
                :label="parsePublicationType(item.publicationType, item.count)"
                hide-details
                class="pl-9 ml-4"
                v-model="searchStore.publicationTypeIdx[i]"
              ></v-checkbox>
            </v-list-group>
            <v-list-group sub-group>
              <template v-slot:activator="{ props }">
                <v-list-item v-bind="props" title="Paketsigel"></v-list-item>
              </template>
              <h6></h6>
              <v-checkbox
                v-for="(item, i) in searchStore.paketSigelIdReceived"
                :key="i"
                :label="ppPaketSigel(item.paketSigel, item.count)"
                hide-details
                class="pl-9 ml-4"
                v-model="searchStore.paketSigelIdIdx[i]"
              ></v-checkbox>
            </v-list-group>
            <v-list-group sub-group>
              <template v-slot:activator="{ props }">
                <v-list-item v-bind="props" title="ZDB-IDs"></v-list-item>
              </template>
              <h6></h6>
              <v-checkbox
                v-for="(item, i) in searchStore.zdbIdReceived"
                :key="i"
                :label="ppZDBId(item.zdbId, item.count)"
                hide-details
                class="pl-9 ml-4"
                v-model="searchStore.zdbIdIdx[i]"
              ></v-checkbox>
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
              <v-checkbox
                v-for="(item, i) in searchStore.accessStateReceived"
                :key="i"
                :label="parseAccessState(item.accessState, item.count)"
                hide-details
                class="pl-9 ml-4"
                v-model="searchStore.accessStateIdx[i]"
              ></v-checkbox>
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
                    :modelValue="temporalValidOnFormatted"
                    prepend-icon="mdi-calendar"
                    v-bind="props"
                    readonly
                  ></v-text-field>
                </template>
                <v-date-picker
                  v-model="searchStore.temporalValidOn"
                  color="primary"
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
                    :modelValue="startEndDateFormatted"
                    prepend-icon="mdi-calendar"
                    v-bind="props"
                    readonly
                    required
                    clearable
                    @change="v$.startDateOrEndDateValue.$touch()"
                    @blur="v$.startDateOrEndDateValue.$touch()"
                    :error-messages="errorTempEventInput"
                  ></v-text-field>
                </template>
                <v-date-picker
                  v-model="
                    searchStore.temporalEventState.startDateOrEndDateValue
                  "
                  color="primary"
                  ><template v-slot:header></template>
                  <v-spacer></v-spacer>
                  <v-btn
                    text="Cancel"
                    color="primary"
                    @click="tempEventMenu = false"
                  >
                  </v-btn>
                  <v-btn
                    text="OK"
                    color="primary"
                    @click="
                      $refs.tempEventMenu.save(
                        searchStore.temporalEventState.startDateOrEndDateValue,
                      )
                    "
                  ></v-btn>
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
                  ></v-checkbox>
                </v-item>
                <v-item>
                  <v-checkbox
                    label="Enddatum"
                    class="pl-9 ml-4"
                    v-model="
                      searchStore.temporalEventState.startDateOrEndDateOption
                    "
                    :error-messages="errorTempEventStartEnd"
                    value="endDate"
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
              ></v-checkbox>
              <v-checkbox
                label="Aktuell"
                hide-details
                class="pl-9 ml-4"
                v-model="searchStore.temporalValidityFilterPresent"
              ></v-checkbox>
              <v-checkbox
                label="Zukunft"
                hide-details
                class="pl-9 ml-4"
                v-model="searchStore.temporalValidityFilterFuture"
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
              ></v-checkbox>
              <v-checkbox
                v-if="searchStore.hasZbwUserAgreement"
                label="ZBW-Nutzungsvereinbarung"
                hide-details
                class="pl-9 ml-4"
                v-model="searchStore.formalRuleUserAgreement"
              ></v-checkbox>
              <v-checkbox
                v-if="searchStore.hasOpenContentLicence"
                label="Open-Content-Licence"
                hide-details
                class="pl-9 ml-4"
                v-model="searchStore.formalRuleOpenContentLicence"
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
              <v-checkbox
                v-for="(item, i) in searchStore.templateNameReceived"
                :key="i"
                :label="ppZDBId(item.templateName, item.count)"
                hide-details
                class="pl-9 ml-4"
                v-model="searchStore.templateNameIdx[i]"
              ></v-checkbox>
            </v-list-group>
          </v-list>
        </v-col>
      </v-row>
    </v-container>
  </v-card>
</template>
