<template>
  <v-card v-if="rights" class="mx-auto" tile>
    <v-card-title class="subheading font-weight-bold">
      Rechteinformationen
    </v-card-title>
    <v-divider></v-divider>
    <v-expansion-panels focusable multiple>
      <v-expansion-panel>
        <v-expansion-panel-header>Allgemeines</v-expansion-panel-header>
        <v-expansion-panel-content>
          <v-container>
            <v-row>
              <v-col>Lizensbedingungen</v-col>
              <v-col>{{ prettyPrint(rights.licenseConditions) }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row>
              <v-col
                >Provenienz-Lizenzinformation/Dokumentation
                Rechteänderungen</v-col
              >
              <v-col>{{ prettyPrint(rights.provenanceLicense) }}</v-col>
              <v-col></v-col>
            </v-row>
          </v-container>
        </v-expansion-panel-content>
      </v-expansion-panel>
    </v-expansion-panels>
  </v-card>
</template>

<script lang="ts">
import Component from "vue-class-component";
import { Prop, Vue } from "vue-property-decorator";
import {
  RightRest,
  RightRestAccessStateEnum,
} from "@/generated-sources/openapi";

@Component
export default class RightsView extends Vue {
  @Prop({ required: true })
  rights!: Array<RightRest>;

  public prettyPrint(value: string): string {
    if (value) {
      return value;
    } else {
      return "Kein Wert vorhanden";
    }
  }
}
</script>

<style scoped></style>
