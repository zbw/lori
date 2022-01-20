<template>
  <v-card v-if="actions" class="mx-auto" tile>
    <v-card-title class="subheading font-weight-bold"> Rechteinformationen </v-card-title>
    <v-divider></v-divider>
    <v-expansion-panels focusable>
      <v-expansion-panel v-for="(action, i) in actions" :key="i">
        <v-expansion-panel-header>{{
          $t(action.actiontype)
        }}</v-expansion-panel-header>
        <v-expansion-panel-content>
          Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do
          eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad
          minim veniam, quis nostrud exercitation ullamco laboris nisi ut
          aliquip ex ea commodo consequat.
        </v-expansion-panel-content>
      </v-expansion-panel>
    </v-expansion-panels>
  </v-card>
</template>

<script lang="ts">
import Component from "vue-class-component";
import { Prop, Vue } from "vue-property-decorator";
import { ActionRest } from "@/generated-sources/openapi";

@Component
export default class RightsView extends Vue {
  @Prop({ required: true })
  actions!: Array<ActionRest>;
}
</script>

<style scoped></style>
