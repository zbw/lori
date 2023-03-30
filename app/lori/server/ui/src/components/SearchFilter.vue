<script lang="ts">
import { computed, defineComponent, reactive, ref, watch } from "vue";
import { useSearchStore } from "@/stores/search";
import { useVuelidate } from "@vuelidate/core";
import { useDialogsStore } from "@/stores/dialogs";

export default defineComponent({
  setup() {
    const searchStore = useSearchStore();
    const temporalEvent = -1;

    const tempEventMenu = ref(false);
    const tempValidOnMenu = ref(false);
    type FormState = {
      tempEventInput: string;
      tempEventStart: boolean;
      temEventEnd: boolean;
    };

    const tempEventCheckForInput: (
      value: string,
      siblings: FormState
    ) => boolean = (value: string, siblings: FormState) => {
      return !(
        ((value == "startDate" || value == "endDate") &&
          siblings.tempEventInput != "") ||
        siblings.tempEventInput != undefined
      );
    };

    const tempEventState = reactive({
      startDateOrEndDateValue: "",
      startDateOrEndDateOption: "",
    });
    const rules = {
      startDateOrEndDateValue: {},
      startDateOrEndDateOption: { tempEventCheckForInput },
    };

    const v$ = useVuelidate(rules, tempEventState);

    watch(tempEventState, (currentValue, oldValue) => {
      searchStore.temporalEventStartDateFilter =
        currentValue.startDateOrEndDateOption == "startDate";
      searchStore.temporalEventEndDateFilter =
        currentValue.startDateOrEndDateOption == "endDate";
      searchStore.temporalEventInput = currentValue.startDateOrEndDateValue;
    });

    const errorTempEventInput = computed(() => {
      const errors: Array<string> = [];
      if (
        v$.value.startDateOrEndDateOption.$invalid &&
        tempEventState.startDateOrEndDateValue == ""
      ) {
        errors.push("Eintrag wird benötigt");
      }
      return errors;
    });

    const errorTempEventStartEnd = computed(() => {
      const errors: Array<string> = [];
      if (
        !v$.value.startDateOrEndDateOption.$invalid &&
        tempEventState.startDateOrEndDateValue != ""
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
        searchStore.temporalEventInput != "" ||
        searchStore.temporalEventStartDateFilter ||
        searchStore.temporalEventEndDateFilter ||
        searchStore.accessStateClosed ||
        searchStore.accessStateOpen ||
        searchStore.accessStateRestricted ||
        searchStore.formalRuleLicenceContract ||
        searchStore.formalRuleOpenContentLicence ||
        searchStore.formalRuleUserAgreement ||
        searchStore.temporalValidityFilterFuture ||
        searchStore.temporalValidityFilterPresent ||
        searchStore.temporalValidityFilterPast ||
        searchStore.temporalValidOn != "" ||
        searchStore.accessStateIdx.filter((element) => element).length > 0 ||
        searchStore.paketSigelIdIdx.filter((element) => element).length > 0 ||
        searchStore.zdbIdIdx.filter((element) => element).length > 0 ||
        searchStore.publicationTypeIdx.filter((element) => element).length >
          0 ||
        searchStore.noRightInformation
      );
    });

    const resetFilter: () => void = () => {
      searchStore.publicationDateFrom = "";
      searchStore.publicationDateTo = "";

      searchStore.accessStateOpen = false;
      searchStore.accessStateRestricted = false;
      searchStore.accessStateClosed = false;

      searchStore.temporalEventInput = "";
      searchStore.temporalEventStartDateFilter = false;
      searchStore.temporalEventEndDateFilter = false;

      searchStore.accessStateClosed = false;
      searchStore.accessStateOpen = false;
      searchStore.accessStateRestricted = false;

      searchStore.formalRuleLicenceContract = false;
      searchStore.formalRuleOpenContentLicence = false;
      searchStore.formalRuleUserAgreement = false;

      searchStore.temporalValidityFilterFuture = false;
      searchStore.temporalValidityFilterPresent = false;
      searchStore.temporalValidityFilterPast = false;

      searchStore.temporalValidOn = "";
      tempEventState.startDateOrEndDateValue = "";
      tempEventState.startDateOrEndDateOption = "";

      searchStore.accessStateIdx = searchStore.accessStateIdx.map(() => false);
      searchStore.paketSigelIdIdx = searchStore.paketSigelIdIdx.map(
        () => false
      );
      searchStore.publicationTypeIdx = searchStore.publicationTypeIdx.map(
        () => false
      );
      searchStore.zdbIdIdx = searchStore.zdbIdIdx.map(() => false);
      searchStore.noRightInformation = false;
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
         * encoded as the interface suggest, for instance 'periodicalPart' is
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

    return {
      canReset,
      errorTempEventStartEnd,
      errorTempEventInput,
      tempEventState,
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
      <v-col cols="4">
        <v-btn color="warning" :disabled="!canReset" @click="resetFilter">
          Alle Filter resetten</v-btn
        >
      </v-col>
    </v-row>
    <v-row>
      <v-col cols="4">
        <v-btn color="blue darken-1" @click="activateBookmarkSaveDialog">
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
      <v-list-group no-action sub-group eager>
        <template v-slot:activator>
          <v-list-item-title>Publikationsjahr</v-list-item-title>
        </template>
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
      </v-list-group>
      <v-row>
        <v-col cols="12">
          <v-list-group no-action sub-group eager>
            <template v-slot:activator>
              <v-list-item-title>Publikationstyp</v-list-item-title>
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
          <v-list-group no-action sub-group eager>
            <template v-slot:activator>
              <v-list-item-title>Paketsigel</v-list-item-title>
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
        </v-col>
      </v-row>
      <v-list-group no-action sub-group eager>
        <template v-slot:activator>
          <v-list-item-title>ZDB-IDs</v-list-item-title>
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
    </v-container>
    <v-card-title>Rechteinformationsfilter</v-card-title>
    <v-container fluid>
      <v-row>
        <v-col cols="12">
          <v-list-group no-action sub-group eager>
            <template v-slot:activator>
              <v-list-item-title>Access-Status</v-list-item-title>
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
          <v-list-group no-action sub-group eager>
            <template v-slot:activator>
              <v-list-item-title>Zeitliche Gültigkeit am</v-list-item-title>
            </template>
            <v-menu
              ref="tempValidOnMenu"
              transition="scale-transition"
              :close-on-content-click="false"
              offset-y
              min-width="auto"
              :return-value.sync="searchStore.temporalValidOn"
            >
              <template v-slot:activator="{ on, attrs }">
                <v-text-field
                  prepend-icon="mdi-calendar"
                  readonly
                  outlined
                  clearable
                  v-bind="attrs"
                  v-on="on"
                  required
                  class="pl-7"
                  v-model="searchStore.temporalValidOn"
                ></v-text-field>
              </template>
              <v-date-picker
                v-model="searchStore.temporalValidOn"
                no-title
                scrollable
              >
                <v-spacer></v-spacer>
                <v-btn text color="primary" @click="tempValidOnMenu = false">
                  Cancel</v-btn
                >
                <v-btn
                  text
                  color="primary"
                  @click="
                    $refs.tempValidOnMenu.save(searchStore.temporalValidOn)
                  "
                >
                  OK</v-btn
                >
              </v-date-picker>
            </v-menu>
          </v-list-group>
          <v-list-group no-action sub-group eager>
            <template v-slot:activator>
              <v-list-item-title>
                Zeitliche Gültigkeit Ereignis
              </v-list-item-title>
            </template>
            <v-menu
              ref="tempEventMenu"
              transition="scale-transition"
              offset-y
              min-width="auto"
              :return-value.sync="tempEventState.startDateOrEndDateValue"
              :close-on-content-click="false"
            >
              <template v-slot:activator="{ on, attrs }">
                <v-text-field
                  prepend-icon="mdi-calendar"
                  readonly
                  outlined
                  clearable
                  v-bind="attrs"
                  v-on="on"
                  required
                  class="pl-7"
                  v-model="tempEventState.startDateOrEndDateValue"
                  @change="v$.startDateOrEndDateValue.$touch()"
                  @blur="v$.startDateOrEndDateValue.$touch()"
                  :error-messages="errorTempEventInput"
                ></v-text-field>
              </template>
              <v-date-picker
                v-model="tempEventState.startDateOrEndDateValue"
                no-title
                scrollable
              >
                <v-spacer></v-spacer>
                <v-btn text color="primary" @click="tempEventMenu = false">
                  Cancel
                </v-btn>
                <v-btn
                  text
                  color="primary"
                  @click="
                    $refs.tempEventMenu.save(
                      tempEventState.startDateOrEndDateValue
                    )
                  "
                >
                  OK
                </v-btn>
              </v-date-picker>
            </v-menu>
            <v-item-group v-model="temporalEvent">
              <v-item>
                <v-checkbox
                  label="Startdatum"
                  class="pl-9 ml-4"
                  hide-details
                  v-model="tempEventState.startDateOrEndDateOption"
                  value="startDate"
                  :error-messages="errorTempEventStartEnd"
                ></v-checkbox>
              </v-item>
              <v-item>
                <v-checkbox
                  label="Enddatum"
                  class="pl-9 ml-4"
                  v-model="tempEventState.startDateOrEndDateOption"
                  :error-messages="errorTempEventStartEnd"
                  value="endDate"
                ></v-checkbox>
              </v-item>
            </v-item-group>
          </v-list-group>
          <v-list-group no-action sub-group eager>
            <template v-slot:activator>
              <v-list-item-title>Zeitliche Gültigkeit</v-list-item-title>
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
          <v-list-group no-action sub-group eager>
            <template v-slot:activator>
              <v-list-item-title>Formale Regelung</v-list-item-title>
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
            <template v-slot:activator>
              <v-list-item-title>Allgemein</v-list-item-title>
            </template>
            <h6></h6>
            <v-checkbox
              label="Keine Rechteeintrag"
              hide-details
              class="pl-9 ml-4"
              v-model="searchStore.noRightInformation"
            ></v-checkbox>
          </v-list-group>
        </v-col>
      </v-row>
    </v-container>
  </v-card>
</template>
