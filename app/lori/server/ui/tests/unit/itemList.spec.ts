import { shallowMount, Wrapper } from "@vue/test-utils";
import { mocked } from "ts-jest/utils";
import ItemList from "@/components/ItemList.vue";
import Vuetify from "vuetify";
import Vue from "vue";
import api from "@/api/api";
import {
  ItemInformation,
  ItemRest,
  MetadataRest,
} from "@/generated-sources/openapi";
import { createTestingPinia } from "@pinia/testing";
import { useSearchStore } from "@/stores/search";

Vue.use(Vuetify);

let wrapper: Wrapper<ItemList, Element>;
jest.mock("@/api/api");

const mockedApi = mocked(api, true);

afterEach(() => {
  jest.clearAllMocks();
  wrapper.destroy();
});

describe("Test ItemList UI", () => {
  it("initial table load is successful", async () => {
    mockedApi.searchQuery.mockReturnValue(
      Promise.resolve({
        itemArray: Array<ItemRest>({
          metadata: {},
          rights: {},
        } as ItemRest),
        totalPages: 25,
      } as ItemInformation)
    );
    wrapper = shallowMount(ItemList, {
      mocks: { api: mockedApi },
      pinia: createTestingPinia({
        stubActions: false,
      }),
    });
    expect((wrapper.vm as any).getAlertLoad().value).toBeFalsy();
    (wrapper.vm as any).startSearch();
    await wrapper.vm.$nextTick();
    expect((wrapper.vm as any).getAlertLoad().value).toBeFalsy();
    expect((wrapper.vm as any).totalPages).toBe(25);
    expect((wrapper.vm as any).tableContentLoading).toBeFalsy();
  });

  it("initial table load fails", async () => {
    mockedApi.searchQuery.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
    });
    wrapper = shallowMount(ItemList, {
      mocks: { api: mockedApi },
      pinia: createTestingPinia({
        stubActions: false,
      }),
    });
    expect((wrapper.vm as any).getAlertLoad().value).toBeFalsy();
    (wrapper.vm as any).startSearch();
    await wrapper.vm.$nextTick();
    expect((wrapper.vm as any).getAlertLoad().value).toBeTruthy();
  });

  it("test search query", async () => {
    // given
    const givenSearchTerm = "foobar";
    const givenMetadata = {
      metadataId: "5",
    } as MetadataRest;
    mockedApi.searchQuery.mockReturnValue(
      Promise.resolve({
        itemArray: Array<ItemRest>({
          metadata: givenMetadata,
          rights: {},
        } as ItemRest),
        totalPages: 25,
        numberOfResults: 1,
      } as ItemInformation)
    );
    wrapper = shallowMount(ItemList, {
      mocks: { api: mockedApi },
      pinia: createTestingPinia({
        stubActions: false,
      }),
    });
    const searchStore = useSearchStore();
    expect(searchStore.lastSearchTerm).toBe("");

    // when
    (wrapper.vm as any).searchTerm = givenSearchTerm;
    (wrapper.vm as any).startSearch();
    await wrapper.vm.$nextTick();
    expect(searchStore.lastSearchTerm).toBe(givenSearchTerm);
    expect((wrapper.vm as any).getAlertLoad().value).toBeFalsy();
    expect((wrapper.vm as any).totalPages).toBe(25);
    expect((wrapper.vm as any).tableContentLoading).toBeFalsy();
    expect((wrapper.vm as any).numberOfResults).toBe(1);
    expect((wrapper.vm as any).items[0].metadata).toBe(givenMetadata);
  });

  it("test search query fails", async () => {
    // given
    mockedApi.searchQuery.mockReturnValue(
      Promise.resolve({
        itemArray: Array<ItemRest>({
          metadata: {},
          rights: {},
        } as ItemRest),
        totalPages: 25,
      } as ItemInformation)
    );
    mockedApi.searchQuery.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
    });
    wrapper = shallowMount(ItemList, {
      mocks: { api: mockedApi },
      pinia: createTestingPinia({
        stubActions: false,
      }),
    });
    // when
    (wrapper.vm as any).startSearch();
    await wrapper.vm.$nextTick();
    expect((wrapper.vm as any).getAlertLoad().value).toBeTruthy();
  });

  it("testPublicationDateFilter", async () => {
    // given
    mockedApi.searchQuery.mockReturnValue(
      Promise.resolve({
        itemArray: Array<ItemRest>({
          metadata: {},
          rights: {},
        } as ItemRest),
        totalPages: 25,
      } as ItemInformation)
    );
    wrapper = shallowMount(ItemList, {
      mocks: { api: mockedApi },
      pinia: createTestingPinia({
        stubActions: false,
      }),
    });
    const searchStore = useSearchStore();

    // when + then
    searchStore.publicationDateFrom = "";
    expect((wrapper.vm as any).buildPublicationDateFilter()).toBeUndefined();

    // when + then
    searchStore.publicationDateFrom = "2022";
    searchStore.publicationDateTo = "";
    expect((wrapper.vm as any).buildPublicationDateFilter()).toBe("2022-");

    // when + then
    searchStore.publicationDateFrom = "";
    searchStore.publicationDateTo = "2023";
    expect((wrapper.vm as any).buildPublicationDateFilter()).toBe("-2023");

    // when + then
    searchStore.publicationDateFrom = "2022";
    searchStore.publicationDateTo = "2023";
    expect((wrapper.vm as any).buildPublicationDateFilter()).toBe("2022-2023");
  });

  it("testPublicationTypeFilter", async () => {
    // given
    mockedApi.searchQuery.mockReturnValue(
      Promise.resolve({
        itemArray: Array<ItemRest>({
          metadata: {},
          rights: {},
        } as ItemRest),
        totalPages: 25,
      } as ItemInformation)
    );
    wrapper = shallowMount(ItemList, {
      mocks: { api: mockedApi },
      pinia: createTestingPinia({
        stubActions: false,
      }),
    });
    const searchStore = useSearchStore();
    // when + then
    expect((wrapper.vm as any).buildPublicationTypeFilter()).toBeUndefined();

    // when

    searchStore.availablePublicationTypes = ["article", "book", "bookPart"];
    searchStore.publicationTypeIdx = [true, false, true];
    // then
    expect((wrapper.vm as any).buildPublicationTypeFilter()).toBe(
      "ARTICLE,BOOK_PART"
    );
  });

  it("testAccessStateFilter", async () => {
    // given
    mockedApi.searchQuery.mockReturnValue(
      Promise.resolve({
        itemArray: Array<ItemRest>({
          metadata: {},
          rights: {},
        } as ItemRest),
        totalPages: 25,
      } as ItemInformation)
    );
    wrapper = shallowMount(ItemList, {
      mocks: { api: mockedApi },
      pinia: createTestingPinia({
        stubActions: false,
      }),
    });
    const searchStore = useSearchStore();
    // when + then
    searchStore.accessStateOpen = false;
    searchStore.accessStateRestricted = false;
    searchStore.accessStateClosed = false;
    expect((wrapper.vm as any).buildAccessStateFilter()).toBeUndefined();

    // when
    searchStore.accessStateOpen = true;
    searchStore.accessStateRestricted = true;
    searchStore.accessStateClosed = true;
    expect((wrapper.vm as any).buildAccessStateFilter()).toBe(
      "OPEN,RESTRICTED,CLOSED"
    );
  });

  it("testTempValFilter", async () => {
    // given
    mockedApi.searchQuery.mockReturnValue(
      Promise.resolve({
        itemArray: Array<ItemRest>({
          metadata: {},
          rights: {},
        } as ItemRest),
        totalPages: 25,
      } as ItemInformation)
    );
    wrapper = shallowMount(ItemList, {
      mocks: { api: mockedApi },
      pinia: createTestingPinia({
        stubActions: false,
      }),
    });
    const searchStore = useSearchStore();
    // when + then
    searchStore.temporalValidityFilterFuture = false;
    searchStore.temporalValidityFilterPast = false;
    searchStore.temporalValidityFilterPresent = false;
    expect((wrapper.vm as any).buildTempValFilter()).toBeUndefined();

    // when
    searchStore.temporalValidityFilterFuture = true;
    searchStore.temporalValidityFilterPast = true;
    searchStore.temporalValidityFilterPresent = true;
    expect((wrapper.vm as any).buildTempValFilter()).toBe(
      "FUTURE,PAST,PRESENT"
    );
  });

  it("testDateAtFilter", async () => {
    // given
    mockedApi.searchQuery.mockReturnValue(
      Promise.resolve({
        itemArray: Array<ItemRest>({
          metadata: {},
          rights: {},
        } as ItemRest),
        totalPages: 25,
      } as ItemInformation)
    );
    wrapper = shallowMount(ItemList, {
      mocks: { api: mockedApi },
      pinia: createTestingPinia({
        stubActions: false,
      }),
    });

    const searchStore = useSearchStore();
    // when + then
    searchStore.temporalEventStartDateFilter = false;
    expect((wrapper.vm as any).buildStartDateAtFilter()).toBeUndefined();
    searchStore.temporalEventStartDateFilter = true;
    searchStore.temporalEventInput = "";
    expect((wrapper.vm as any).buildStartDateAtFilter()).toBeUndefined();
    searchStore.temporalEventInput = "2022-01-01";
    expect((wrapper.vm as any).buildStartDateAtFilter()).toBe("2022-01-01");

    // when + then
    searchStore.temporalEventEndDateFilter = false;
    expect((wrapper.vm as any).buildEndDateAtFilter()).toBeUndefined();
    searchStore.temporalEventEndDateFilter = true;
    searchStore.temporalEventInput = "";
    expect((wrapper.vm as any).buildEndDateAtFilter()).toBeUndefined();
    searchStore.temporalEventInput = "2022-01-01";
    expect((wrapper.vm as any).buildEndDateAtFilter()).toBe("2022-01-01");
  });

  it("testFormalRuleFilter", async () => {
    // given
    mockedApi.searchQuery.mockReturnValue(
      Promise.resolve({
        itemArray: Array<ItemRest>({
          metadata: {},
          rights: {},
        } as ItemRest),
        totalPages: 25,
      } as ItemInformation)
    );
    wrapper = shallowMount(ItemList, {
      mocks: { api: mockedApi },
      pinia: createTestingPinia({
        stubActions: false,
      }),
    });
    const searchStore = useSearchStore();
    // when + then
    searchStore.formalRuleLicenceContract = false;
    searchStore.formalRuleOpenContentLicence = false;
    searchStore.formalRuleUserAgreement = false;
    expect((wrapper.vm as any).buildFormalRuleFilter()).toBeUndefined();

    // when
    searchStore.formalRuleLicenceContract = true;
    searchStore.formalRuleOpenContentLicence = true;
    searchStore.formalRuleUserAgreement = true;
    expect((wrapper.vm as any).buildFormalRuleFilter()).toBe(
      "LICENCE_CONTRACT,OPEN_CONTENT_LICENCE,ZBW_USER_AGREEMENT"
    );
  });

  it("testValidOnFilter", async () => {
    // given
    mockedApi.searchQuery.mockReturnValue(
      Promise.resolve({
        itemArray: Array<ItemRest>({
          metadata: {},
          rights: {},
        } as ItemRest),
        totalPages: 25,
      } as ItemInformation)
    );
    wrapper = shallowMount(ItemList, {
      mocks: { api: mockedApi },
      pinia: createTestingPinia({
        stubActions: false,
      }),
    });
    const searchStore = useSearchStore();
    // when + then
    searchStore.temporalValidOn = "";
    expect((wrapper.vm as any).buildValidOnFilter()).toBeUndefined();

    // when
    searchStore.temporalValidOn = "2022-01-01";
    expect((wrapper.vm as any).buildValidOnFilter()).toBe("2022-01-01");
  });

  it("testPaketSigelIdFilter", async () => {
    // given
    mockedApi.searchQuery.mockReturnValue(
      Promise.resolve({
        itemArray: Array<ItemRest>({
          metadata: {},
          rights: {},
        } as ItemRest),
        totalPages: 25,
      } as ItemInformation)
    );
    wrapper = shallowMount(ItemList, {
      mocks: { api: mockedApi },
      pinia: createTestingPinia({
        stubActions: false,
      }),
    });

    const searchStore = useSearchStore();
    searchStore.availablePaketSigelIds = ["foo", "bar", "baz"];
    searchStore.paketSigelIdIdx = [true, false, true];

    // when + then
    expect((wrapper.vm as any).buildPaketSigelIdFilter()).toBe("foo,baz");

    searchStore.paketSigelIdIdx = [];
    // when + then
    expect((wrapper.vm as any).buildPaketSigelIdFilter()).toBeUndefined();
  });

  it("testZDBIdFilter", async () => {
    // given
    mockedApi.searchQuery.mockReturnValue(
      Promise.resolve({
        itemArray: Array<ItemRest>({
          metadata: {},
          rights: {},
        } as ItemRest),
        totalPages: 25,
      } as ItemInformation)
    );
    wrapper = shallowMount(ItemList, {
      mocks: { api: mockedApi },
      pinia: createTestingPinia({
        stubActions: false,
      }),
    });

    const searchStore = useSearchStore();
    searchStore.availableZDBIds = ["foo", "bar", "baz"];
    searchStore.zdbIdIdx = [true, false, true];

    // when + then
    expect((wrapper.vm as any).buildZDBIdFilter()).toBe("foo,baz");

    searchStore.availableZDBIds = [];
    // when + then
    expect((wrapper.vm as any).buildPaketSigelIdFilter()).toBeUndefined();
  });
});
