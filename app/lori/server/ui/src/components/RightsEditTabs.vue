<template>
  <v-card>
    <v-toolbar color="cyan" dark flat :key="renderKey">
      <v-toolbar-title>
        Editiere Rechte für {{ this.metadataId }}
      </v-toolbar-title>
      <v-spacer></v-spacer>
      <template v-slot:extension>
        <v-tabs v-model="tab" align-with-title show-arrows>
          <v-tabs-slider color="yellow"></v-tabs-slider>

          <v-tab v-for="name in tabNames" :key="name">
            {{ name }}
          </v-tab>
        </v-tabs>
      </template>
    </v-toolbar>

    <v-tabs-items v-model="tab">
      <v-alert
        @close="resetLastUpdateSuccessful"
        v-model="lastUpdateSuccessful"
        dismissible
        text
        type="success"
      >
        Rechteinformation {{ this.lastUpdatedRight }} erfolgreich geupdated für
        Item {{ this.metadataId }}.
      </v-alert>
      <v-alert
        @close="resetLastDeletionSuccessful"
        v-model="lastDeletionSuccessful"
        dismissible
        text
        type="success"
      >
        Rechteinformation {{ this.lastDeletedRight }} erfolgreich gelöscht für
        Item {{ this.metadataId }}.
      </v-alert>
      <v-tab-item v-for="(item, index) in rights" :key="item.rightId">
        <RightsEditDialog
          :activated="true"
          :right="item"
          :index="index"
          :isNew="false"
          :metadataId="metadataId"
          v-on:deleteSuccessful="deleteSuccessful"
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
  private renderKey = 0;
  private tab = null;
  private lastDeletedRight = "";
  private lastDeletionSuccessful = false;
  private lastUpdatedRight = "";
  private lastUpdateSuccessful = false;

  // Methods
  public deleteSuccessful(index: number, rightIdDeleted: string | undefined) {
    this.rights.splice(index, 1);
    this.renderKey += 1;
    this.lastDeletionSuccessful = true;
    this.lastDeletedRight = rightIdDeleted != undefined ? rightIdDeleted : "";
    this.$emit("deleteSuccessful", index);
  }

  public resetLastDeletionSuccessful() {
    this.lastDeletionSuccessful = false;
  }

  public tabDialogClosed() {
    this.$emit("tabDialogClosed");
  }

  public updateSuccessful(right: RightRest, index: number): void {
    this.lastUpdateSuccessful = true;
    this.lastUpdatedRight = right.rightId != undefined ? right.rightId : "";
    this.$emit("updateSuccessful", right, index);
  }

  public resetLastUpdateSuccessful() {
    this.lastUpdateSuccessful = false;
  }

  // Computed properties
  get tabNames() {
    return this.rights.map((r) => r.rightId);
  }
}
</script>

<style scoped></style>
