import { shallowMount, Wrapper } from "@vue/test-utils";
import { mocked } from "ts-jest/utils";
import ItemList from "@/components/ItemList.vue";
import Vuetify from "vuetify";
import Vue from "vue";
import api from "@/api/api";
import searchquerybulder from "@/utils/searchquerybuilder";
import {
  AccessStateRest,
  ItemInformation,
  ItemRest,
  MetadataRest,
} from "@/generated-sources/openapi";
import { createTestingPinia } from "@pinia/testing";
import { useSearchStore } from "@/stores/search";
import searchquerybuilder from "@/utils/searchquerybuilder";

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
      response: {
        status: 500,
        statusText: "Internal Server Error",
      },
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
      response: {
        status: 500,
        statusText: "Internal Server Error",
      },
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
    expect(
      searchquerybuilder.buildPublicationDateFilter(searchStore)
    ).toBeUndefined();

    // when + then
    searchStore.publicationDateFrom = "2022";
    searchStore.publicationDateTo = "";
    expect(searchquerybuilder.buildPublicationDateFilter(searchStore)).toBe(
      "2022-"
    );

    // when + then
    searchStore.publicationDateFrom = "";
    searchStore.publicationDateTo = "2023";
    expect(searchquerybuilder.buildPublicationDateFilter(searchStore)).toBe(
      "-2023"
    );

    // when + then
    searchStore.publicationDateFrom = "2022";
    searchStore.publicationDateTo = "2023";
    expect(searchquerybuilder.buildPublicationDateFilter(searchStore)).toBe(
      "2022-2023"
    );
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
    expect(
      searchquerybuilder.buildPublicationTypeFilter(searchStore)
    ).toBeUndefined();

    // when
    searchStore.publicationTypeReceived = [
      { count: 2, publicationType: "article" },
      { count: 2, publicationType: "book" },
      { count: 2, publicationType: "book_part" },
    ];
    searchStore.publicationTypeIdx = [true, false, true];
    // then
    expect(searchquerybuilder.buildPublicationTypeFilter(searchStore)).toBe(
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
    expect(
      searchquerybuilder.buildAccessStateFilter(searchStore)
    ).toBeUndefined();

    // when

    searchStore.accessStateReceived = [
      { accessState: AccessStateRest.Open, count: 1 },
      { accessState: AccessStateRest.Restricted, count: 1 },
      { accessState: AccessStateRest.Closed, count: 1 },
    ];
    searchStore.accessStateIdx = [true, true, true];
    expect(searchquerybuilder.buildAccessStateFilter(searchStore)).toBe(
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
    expect(searchquerybuilder.buildTempValFilter(searchStore)).toBeUndefined();

    // when
    searchStore.temporalValidityFilterFuture = true;
    searchStore.temporalValidityFilterPast = true;
    searchStore.temporalValidityFilterPresent = true;
    expect(searchquerybuilder.buildTempValFilter(searchStore)).toBe(
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
    expect(
      searchquerybuilder.buildStartDateAtFilter(searchStore)
    ).toBeUndefined();
    searchStore.temporalEventStartDateFilter = true;
    searchStore.temporalEventInput = "";
    expect(
      searchquerybuilder.buildStartDateAtFilter(searchStore)
    ).toBeUndefined();
    searchStore.temporalEventInput = "2022-01-01";
    expect(searchquerybuilder.buildStartDateAtFilter(searchStore)).toBe(
      "2022-01-01"
    );

    // when + then
    searchStore.temporalEventEndDateFilter = false;
    expect(
      searchquerybuilder.buildEndDateAtFilter(searchStore)
    ).toBeUndefined();
    searchStore.temporalEventEndDateFilter = true;
    searchStore.temporalEventInput = "";
    expect(
      searchquerybuilder.buildEndDateAtFilter(searchStore)
    ).toBeUndefined();
    searchStore.temporalEventInput = "2022-01-01";
    expect(searchquerybuilder.buildEndDateAtFilter(searchStore)).toBe(
      "2022-01-01"
    );
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
    expect(
      searchquerybuilder.buildFormalRuleFilter(searchStore)
    ).toBeUndefined();

    // when
    searchStore.formalRuleLicenceContract = true;
    searchStore.formalRuleOpenContentLicence = true;
    searchStore.formalRuleUserAgreement = true;
    expect(searchquerybuilder.buildFormalRuleFilter(searchStore)).toBe(
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
    expect(searchquerybuilder.buildValidOnFilter(searchStore)).toBeUndefined();

    // when
    searchStore.temporalValidOn = "2022-01-01";
    expect(searchquerybuilder.buildValidOnFilter(searchStore)).toBe(
      "2022-01-01"
    );
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
    searchStore.paketSigelIdReceived = [
      { paketSigel: "foo", count: 1 },
      { paketSigel: "bar", count: 1 },
      { paketSigel: "baz", count: 1 },
    ];
    searchStore.paketSigelIdIdx = [true, false, true];

    // when + then
    expect(searchquerybuilder.buildPaketSigelIdFilter(searchStore)).toBe(
      "foo,baz"
    );

    searchStore.paketSigelIdIdx = [];
    // when + then
    expect(
      searchquerybuilder.buildPaketSigelIdFilter(searchStore)
    ).toBeUndefined();
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
    searchStore.zdbIdReceived = [
      { zdbId: "foo", count: 1 },
      { zdbId: "bar", count: 1 },
      { zdbId: "baz", count: 1 },
    ];
    searchStore.zdbIdIdx = [true, false, true];

    // when + then
    expect(searchquerybuilder.buildZDBIdFilter(searchStore)).toBe("foo,baz");

    searchStore.zdbIdReceived = [];
    // when + then
    expect(
      searchquerybuilder.buildPaketSigelIdFilter(searchStore)
    ).toBeUndefined();
  });
});
