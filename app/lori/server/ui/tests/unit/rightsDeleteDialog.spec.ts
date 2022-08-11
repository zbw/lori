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
  jest.clearAllMocks();
});

describe("Test RightsDeleteDialog", () => {
  it("deleteRight successful", async () => {
    // given
    const index = 5;
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
        index: index,
        metadataId: "400",
      },
    });
    const historyStore = useHistoryStore();
    expect(historyStore.numberEntries).toBe(0);

    // when
    (wrapper.vm as any).deleteRight();
    await wrapper.vm.$nextTick();

    // then
    expect(historyStore.numberEntries).toBe(1);
    // Verify emit of event
    expect(wrapper.emitted("deleteSuccessful")?.at(0)[0]).toBe(index);
  });

  it("deleteRight unsuccessful", async () => {
    // given
    mockedApi.deleteItemRelation.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
    });
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
        index: 4,
        metadataId: "400",
      },
    });
    const historyStore = useHistoryStore();
    expect(historyStore.numberEntries).toBe(0);

    // when
    (wrapper.vm as any).deleteRight();

    // then
    expect(historyStore.numberEntries).toBe(0);
    expect(wrapper.emitted("deleteSuccessful")?.length).toBeUndefined();
    await wrapper.vm.$nextTick();
    // Just wait another time 'cause then it works
    await wrapper.vm.$nextTick();
    expect((wrapper.vm as any).deleteAlertError).toBeTruthy();
  });
});
