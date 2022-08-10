import { createLocalVue, shallowMount, Wrapper } from "@vue/test-utils";
import { mocked } from "ts-jest/utils";
import Vuetify from "vuetify";
import Vue from "vue";
import api from "@/api/api";
import RightsDeleteDialog from "@/components/RightsDeleteDialog.vue";
import { PiniaVuePlugin } from "pinia";
import { createTestingPinia } from "@pinia/testing";
import { useHistoryStore } from "@/stores/history";
import {
  RightRest,
  RightRestAccessStateEnum,
} from "@/generated-sources/openapi";

const localVue = createLocalVue();
localVue.use(PiniaVuePlugin);

Vue.use(Vuetify);

let wrapper: Wrapper<RightsDeleteDialog, Element>;
jest.mock("@/api/api");

const mockedApi = mocked(api, true);

afterEach(() => {
  wrapper.destroy();
});

describe("Test RightsDeleteDialog", () => {
  it("deletion updates history store", async () => {
    mockedApi.deleteItemRelation.mockReturnValue(Promise.resolve());
    wrapper = shallowMount(RightsDeleteDialog, {
      localVue: localVue,
      mocks: { api: mockedApi },
      pinia: createTestingPinia({
        stubActions: false,
      }),
      propsData: {
        right: {
          rightId: "12",
          accessState: RightRestAccessStateEnum.Open,
          startDate: new Date(2022, 12, 12),
        } as RightRest,
        index: 0,
        metadataId: "400",
      },
    });
    const historyStore = useHistoryStore();

    expect(historyStore.numberEntries).toBe(0);
    (wrapper.vm as any).deleteRight();
    await wrapper.vm.$nextTick();
    expect(historyStore.numberEntries).toBe(1);
  });
});
