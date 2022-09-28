<script lang="ts">
import { defineComponent } from "vue";
import { useSearchStore } from "@/stores/search";

export default defineComponent({
  setup() {
    const searchStore = useSearchStore();

    return {
      searchStore,
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
          <v-list-item-title>Erscheinungsjahr</v-list-item-title>
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
              <v-list-item-title>Publikationsform</v-list-item-title>
            </template>
            <v-checkbox
              label="Aufsatz/Article"
              hide-details
              class="pl-9 ml-4"
            ></v-checkbox>
            <v-checkbox
              label="Buchaufsatz/Book Part"
              hide-details
              class="pl-9 ml-4"
            ></v-checkbox>
            <v-checkbox
              label="Konferenzschrift/Conference Paper"
              hide-details
              class="pl-9 ml-4"
            ></v-checkbox>
            <v-checkbox
              label="Zeitschriftenband/Periodical Part"
              hide-details
              class="pl-9 ml-4"
            ></v-checkbox>
            <v-checkbox
              label="Forschungsbericht/Research Report"
              hide-details
              class="pl-9 ml-4"
            ></v-checkbox>
            <v-checkbox
              label="Konferenzband/Proceeding"
              hide-details
              class="pl-9 ml-4"
            ></v-checkbox>
            <v-checkbox
              label="Working Paper"
              hide-details
              class="pl-9 ml-4"
            ></v-checkbox>
            <v-checkbox
              label="Buch/Book"
              hide-details
              class="pl-9 ml-4"
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
          <v-list-item-title>ZBD-IDs</v-list-item-title>
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
            ></v-checkbox>
            <v-checkbox
              label="Restricted Access"
              hide-details
              class="pl-9 ml-4"
            ></v-checkbox>
            <v-checkbox
              label="Closed Access"
              hide-details
              class="pl-9 ml-4"
            ></v-checkbox>
          </v-list-group>
          <v-list-group no-action sub-group eager>
            <template v-slot:activator>
              <v-list-item-title>Zeitliche Gütligkeit am</v-list-item-title>
            </template>
            <v-menu
              ref="menuStart"
              transition="scale-transition"
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
                <v-btn text color="primary"> Cancel </v-btn>
                <v-btn text color="primary"> OK </v-btn>
              </v-date-picker>
            </v-menu>
          </v-list-group>
          <v-list-group no-action sub-group eager>
            <template v-slot:activator>
              <v-list-item-title
                >Zeitliche Gütligkeit Ereignis</v-list-item-title
              >
            </template>
            <v-menu
              ref="menuStart"
              transition="scale-transition"
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
                <v-btn text color="primary"> Cancel </v-btn>
                <v-btn text color="primary"> OK </v-btn>
              </v-date-picker>
            </v-menu>
            <v-checkbox
              label="Startdatum"
              hide-details
              class="pl-9 ml-4"
            ></v-checkbox>
            <v-checkbox
              label="Enddatum"
              hide-details
              class="pl-9 ml-4"
            ></v-checkbox>
          </v-list-group>
          <v-list-group no-action sub-group eager>
            <template v-slot:activator>
              <v-list-item-title>Zeitliche Gültigkeit</v-list-item-title>
            </template>
            <v-checkbox
              label="Vergangenheit"
              hide-details
              class="pl-9 ml-4"
            ></v-checkbox>
            <v-checkbox
              label="Aktuell"
              hide-details
              class="pl-9 ml-4"
            ></v-checkbox>
            <v-checkbox
              label="Zukunft"
              hide-details
              class="pl-9 ml-4"
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
            ></v-checkbox>
            <v-checkbox
              label="ZBW-Nutzungsvereinbarung Access"
              hide-details
              class="pl-9 ml-4"
            ></v-checkbox>
            <v-checkbox
              label="Open-Content-Licence"
              hide-details
              class="pl-9 ml-4"
            ></v-checkbox>
          </v-list-group>
        </v-col>
      </v-row>
    </v-container>
  </v-card>
</template>
