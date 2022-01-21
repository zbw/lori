<template>
  <v-card v-if="actions" class="mx-auto" tile>
    <v-card-title class="subheading font-weight-bold">
      Rechteinformationen
    </v-card-title>
    <v-divider></v-divider>
    <v-expansion-panels focusable>
      <v-expansion-panel v-for="(action, i) in actions" :key="i">
        <v-expansion-panel-header>{{
          $t(action.actiontype)
        }}</v-expansion-panel-header>
        <v-expansion-panel-content>
          <v-container>
            <v-row v-if="action.actiontype === 'read'">
              <v-col> Zugriffstatus </v-col>
              <v-col>
                {{ prettyPrint(accessState) }}
              </v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-for="(restriction, j) in action.restrictions" :key="j">
              <v-col>
                {{ $t(restriction.restrictiontype) }}
              </v-col>
              <v-col v-if="restriction.attributetype === 'parts'"> </v-col>
              <v-col v-else-if="restriction.attributetype === 'groups'">
              </v-col>
              <v-col v-else>
                {{ $t(restriction.attributetype) }}
              </v-col>
              <v-col v-if="restriction.attributetype === 'groups'">
                {{ restriction.attributevalues }}
              </v-col>
              <v-col v-else-if="restriction.attributetype === 'parts'">
                {{ restriction.attributevalues }}
              </v-col>
              <v-col v-else>
                {{ restriction.attributevalues[0] }}
              </v-col>
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
  ActionRest,
  ItemRestAccessStateEnum,
} from "@/generated-sources/openapi";

@Component
export default class RightsView extends Vue {
  @Prop({ required: true })
  actions!: Array<ActionRest>;
  @Prop({ required: false })
  accessState!: ItemRestAccessStateEnum;

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
