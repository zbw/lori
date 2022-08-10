import { defineStore } from "pinia";
import { computed, ref } from "vue";

export const useHistoryStore = defineStore("history", () => {
  const history = ref([] as HistoryEntry[]);

  function addEntry(entry: HistoryEntry) {
    history.value.push(entry);
  }

  const numberEntries = computed(() => history.value.length);

  return {
    history,
    numberEntries,
    addEntry,
  };
});

export enum ChangeType {
  CREATED = "CREATED",
  UPDATED = "UPDATED",
  DELETED = "DELETED",
}

export interface HistoryEntry {
  type: ChangeType;
  rightId: string | undefined;
}
