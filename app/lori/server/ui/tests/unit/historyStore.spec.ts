import { setActivePinia, createPinia } from "pinia";
import { ChangeType, HistoryEntry, useHistoryStore } from "@/stores/history";

describe("History Store", () => {
  beforeEach(() => {
    // creates a fresh pinia and make it active so it's automatically picked
    // up by any useStore() call without having to pass it to it:
    // `useStore(pinia)`
    setActivePinia(createPinia());
  });

  it("addEntry", () => {
    const history = useHistoryStore();
    expect(history.history).toStrictEqual([] as HistoryEntry[]);
    const entry1 = {
      type: ChangeType.CREATED,
      rightId: "123",
    };
    const entry2 = {
      type: ChangeType.CREATED,
      rightId: "124",
    };
    history.addEntry(entry1);
    expect(history.history).toStrictEqual([entry1] as HistoryEntry[]);
    expect(history.numberEntries).toBe(1);
    history.addEntry(entry2);
    expect(history.history).toStrictEqual([entry1, entry2] as HistoryEntry[]);
    expect(history.numberEntries).toBe(2);
  });
});
