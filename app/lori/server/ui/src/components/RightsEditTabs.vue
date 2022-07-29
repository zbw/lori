<template>
  <v-card>
    <v-toolbar color="cyan" dark flat>
      <v-toolbar-title>Editiere Rechte</v-toolbar-title>
      <v-spacer></v-spacer>
      <template v-slot:extension>
        <v-tabs v-model="tab" align-with-title>
          <v-tabs-slider color="yellow"></v-tabs-slider>

          <v-tab v-for="name in tabNames" :key="name">
            {{ name }}
          </v-tab>
        </v-tabs>
      </template>
    </v-toolbar>

    <v-tabs-items v-model="tab">
      <v-tab-item v-for="(item, index) in rights" :key="item.rightId">
        <RightsEditDialog
          :activated="true"
          :right="item"
          :index="index"
          :isNew="false"
          :metadataId="metadataId"
          v-on:editDialogClosed="tabDialogClosed"
          v-on:updateSuccessful="updateSuccessful"
        ></RightsEditDialog>
      </v-tab-item>
    </v-tabs-items>
  </v-card>
</template>

<script lang="ts">
import Component from "vue-class-component";
import RightsEditDialog from "@/components/RightsEditDialog.vue";
import { Prop, Vue } from "vue-property-decorator";
import { RightRest } from "@/generated-sources/openapi";

@Component({
  components: { RightsEditDialog },
})
export default class RightsEditTabs extends Vue {
  @Prop({ required: true })
  rights!: Array<RightRest>;
  @Prop({ required: true })
  metadataId!: string;

  // Data properties
  private tab = null;

  public tabDialogClosed() {
    this.$emit("tabDialogClosed");
  }

  public updateSuccessful(right: RightRest, index: number): void {
    this.$emit("updateSuccessful", right, index);
  }

  // Computed properties
  get tabNames() {
    return this.rights.map((r) => r.rightId);
  }
}
</script>

<style scoped></style>
