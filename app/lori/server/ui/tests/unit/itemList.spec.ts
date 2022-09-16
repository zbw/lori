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
    mockedApi.getList.mockReturnValue(
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
    (wrapper.vm as any).retrieveItemInformation();
    await wrapper.vm.$nextTick();
    expect((wrapper.vm as any).getAlertLoad().value).toBeFalsy();
    expect((wrapper.vm as any).totalPages).toBe(25);
    expect((wrapper.vm as any).tableContentLoading).toBeFalsy();
  });

  it("initial table load fails", async () => {
    mockedApi.getList.mockRejectedValue({
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
    (wrapper.vm as any).retrieveItemInformation();
    await wrapper.vm.$nextTick();
    expect((wrapper.vm as any).getAlertLoad().value).toBeTruthy();
  });

  it("test search query", async () => {
    // given
    const givenSearchTerm = "foobar";
    const givenMetadata = {
      metadataId: "5",
    } as MetadataRest;
    mockedApi.getList.mockReturnValue(
      Promise.resolve({
        itemArray: Array<ItemRest>({
          metadata: {},
          rights: {},
        } as ItemRest),
        totalPages: 25,
      } as ItemInformation)
    );
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
    mockedApi.getList.mockReturnValue(
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
});
