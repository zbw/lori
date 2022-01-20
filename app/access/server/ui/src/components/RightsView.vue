<template>
  <v-expansion-panels focusable>
    <v-expansion-panel v-for="(action, i) in actions" :key="i">
      <v-expansion-panel-header>{{ action }}</v-expansion-panel-header>
      <v-expansion-panel-content>
        Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod
        tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim
        veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea
        commodo consequat.
      </v-expansion-panel-content>
    </v-expansion-panel>
  </v-expansion-panels>
</template>

<script lang="ts">
import Component from "vue-class-component";
import { Prop, Vue } from "vue-property-decorator";
import { ActionRest } from "@/generated-sources/openapi";

@Component
export default class RightsView extends Vue {
  @Prop({ required: true })
  actions!: Array<ActionRest>;

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
