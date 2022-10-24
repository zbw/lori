<script lang="ts">
import { computed, defineComponent, reactive, ref, watch } from "vue";
import { useSearchStore } from "@/stores/search";
import { useVuelidate } from "@vuelidate/core";

export default defineComponent({
  setup() {
    const searchStore = useSearchStore();
    const temporalEvent = -1;

    const tempEventMenu = ref(false);
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

    return {
      errorTempEventStartEnd,
      errorTempEventInput,
      tempEventState,
      temporalEvent,
      tempEventMenu,
      searchStore,
      v$,
    };
  },
});
</script>
<template>
  <v-card height="100%">
    <v-card-title>Publikationsfilter</v-card-title>
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
            <v-checkbox
              label="Aufsatz/Article"
              hide-details
              class="pl-9 ml-4"
              v-model="searchStore.publicationTypeArticle"
              data-test="foobar"
            ></v-checkbox>
            <v-checkbox
              label="Buchaufsatz/Book Part"
              hide-details
              class="pl-9 ml-4"
              v-model="searchStore.publicationTypeBookPart"
            ></v-checkbox>
            <v-checkbox
              label="Konferenzschrift/Conference Paper"
              hide-details
              class="pl-9 ml-4"
              v-model="searchStore.publicationTypeConferencePaper"
            ></v-checkbox>
            <v-checkbox
              label="Zeitschriftenband/Periodical Part"
              hide-details
              class="pl-9 ml-4"
              v-model="searchStore.publicationTypePeriodicalPart"
            ></v-checkbox>
            <v-checkbox
              label="Forschungsbericht/Research Report"
              hide-details
              class="pl-9 ml-4"
              v-model="searchStore.publicationTypeResearchReport"
            ></v-checkbox>
            <v-checkbox
              label="Konferenzband/Proceeding"
              hide-details
              class="pl-9 ml-4"
              v-model="searchStore.publicationTypeProceedings"
            ></v-checkbox>
            <v-checkbox
              label="Working Paper"
              hide-details
              class="pl-9 ml-4"
              v-model="searchStore.publicationTypeWorkingPaper"
            ></v-checkbox>
            <v-checkbox
              label="Buch/Book"
              hide-details
              class="pl-9 ml-4"
              v-model="searchStore.publicationTypeBook"
            ></v-checkbox>
          </v-list-group>
          <v-list-group no-action sub-group eager>
            <template v-slot:activator>
              <v-list-item-title>Paketsigel</v-list-item-title>
            </template>
          </v-list-group>
        </v-col>
      </v-row>
      <v-list-group no-action sub-group eager>
        <template v-slot:activator>
          <v-list-item-title>ZDB-IDs</v-list-item-title>
        </template>
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
            <v-checkbox
              label="Open Access"
              hide-details
              class="pl-9 ml-4"
              v-model="searchStore.accessStateOpen"
            ></v-checkbox>
            <v-checkbox
              label="Restricted Access"
              hide-details
              class="pl-9 ml-4"
              v-model="searchStore.accessStateRestricted"
            ></v-checkbox>
            <v-checkbox
              label="Closed Access"
              hide-details
              class="pl-9 ml-4"
              v-model="searchStore.accessStateClosed"
            ></v-checkbox>
          </v-list-group>
          <v-list-group no-action sub-group eager>
            <template v-slot:activator>
              <v-list-item-title>Zeitliche Gütligkeit am</v-list-item-title>
            </template>
            <v-menu
              transition="scale-transition"
              :close-on-content-click="false"
              offset-y
              min-width="auto"
            >
              <template v-slot:activator="{ on, attrs }">
                <v-text-field
                  prepend-icon="mdi-calendar"
                  readonly
                  outlined
                  v-bind="attrs"
                  v-on="on"
                  required
                  class="pl-7"
                ></v-text-field>
              </template>
              <v-date-picker no-title scrollable>
                <v-spacer></v-spacer>
                <v-btn text color="primary"> Cancel</v-btn>
                <v-btn text color="primary"> OK</v-btn>
              </v-date-picker>
            </v-menu>
          </v-list-group>
          <v-list-group no-action sub-group eager>
            <template v-slot:activator>
              <v-list-item-title
                >Zeitliche Gütligkeit Ereignis
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
            <v-checkbox
              label="Lizenzvertrag"
              hide-details
              class="pl-9 ml-4"
              v-model="searchStore.formalRuleLicenceContract"
            ></v-checkbox>
            <v-checkbox
              label="ZBW-Nutzungsvereinbarung"
              hide-details
              class="pl-9 ml-4"
              v-model="searchStore.formalRuleUserAgreement"
            ></v-checkbox>
            <v-checkbox
              label="Open-Content-Licence"
              hide-details
              class="pl-9 ml-4"
              v-model="searchStore.formalRuleOpenContentLicence"
            ></v-checkbox>
          </v-list-group>
        </v-col>
      </v-row>
    </v-container>
  </v-card>
</template>
