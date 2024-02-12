//import { createLocalVue, shallowMount, Wrapper } from "@vue/test-utils";
//import { mocked } from "ts-jest/utils";
//import Vuetify from "vuetify";
//import Vue from "vue";
//import api from "@/api/api";
//import RightsDeleteDialog from "@/components/RightsDeleteDialog.vue";
//import { PiniaVuePlugin } from "pinia";
//import { createTestingPinia } from "@pinia/testing";
//import { useHistoryStore } from "@/stores/history";
//import templateApi from "@/api/templateApi";
//
//const localVue = createLocalVue();
//localVue.use(PiniaVuePlugin);
//
//Vue.use(Vuetify);
//
//let wrapper: Wrapper<RightsDeleteDialog, Element>;
//jest.mock("@/api/api");
//jest.mock("@/api/templateApi");
//const mockedApi = mocked(api, true);
//const mockedTemplateApi = mocked(templateApi, true);
//
//afterEach(() => {
//  wrapper.destroy();
//  jest.clearAllMocks();
//});
//
//describe("Test RightsDeleteDialog", () => {
//  it("deleteRight successful", async () => {
//    // given
//    const index = 5;
//    mockedApi.deleteItemRelation.mockReturnValue(Promise.resolve());
//    wrapper = shallowMount(RightsDeleteDialog, {
//      localVue: localVue,
//      mocks: { api: mockedApi },
//      pinia: createTestingPinia({
//        stubActions: false,
//      }),
//      propsData: {
//        rightId: "12",
//        index: index,
//        metadataId: "400",
//      },
//    });
//    const historyStore = useHistoryStore();
//    expect(historyStore.numberEntries).toBe(0);
//
//    // when
//    (wrapper.vm as any).deleteRight();
//    await wrapper.vm.$nextTick();
//
//    // then
//    expect(historyStore.numberEntries).toBe(1);
//    // Verify emit of event
//    expect(wrapper.emitted("deleteSuccessful")?.at(0)[0]).toBe(index);
//  });
//
//  it("deleteRight unsuccessful", async () => {
//    // given
//    mockedApi.deleteItemRelation.mockRejectedValue({
//      response: {
//        status: 500,
//        statusText: "Internal Server Error",
//      },
//    });
//    wrapper = shallowMount(RightsDeleteDialog, {
//      localVue: localVue,
//      mocks: { api: mockedApi },
//      pinia: createTestingPinia({
//        stubActions: false,
//      }),
//      propsData: {
//        rightId: "12",
//        index: 4,
//        metadataId: "400",
//      },
//    });
//    const historyStore = useHistoryStore();
//    expect(historyStore.numberEntries).toBe(0);
//
//    // when
//    (wrapper.vm as any).deleteRight();
//
//    // then
//    expect(historyStore.numberEntries).toBe(0);
//    expect(wrapper.emitted("deleteSuccessful")?.length).toBeUndefined();
//    await wrapper.vm.$nextTick();
//    // Just wait another time 'cause then it works
//    await wrapper.vm.$nextTick();
//    expect((wrapper.vm as any).deleteAlertError).toBeTruthy();
//  });
//
//  it("deleteTemplate successful", async () => {
//    // given
//    const index = 5;
//    mockedTemplateApi.deleteTemplate.mockReturnValue(Promise.resolve());
//    wrapper = shallowMount(RightsDeleteDialog, {
//      localVue: localVue,
//      mocks: { api: mockedApi },
//      pinia: createTestingPinia({
//        stubActions: false,
//      }),
//      propsData: {
//        rightId: "12",
//        index: index,
//        metadataId: "400",
//        isTemplate: true,
//      },
//    });
//    // when
//    (wrapper.vm as any).deleteTemplate();
//    await wrapper.vm.$nextTick();
//
//    // Verify emit of event
//    expect(wrapper.emitted("templateDeleteSuccessful")).toStrictEqual([[]]);
//  });
//
//  it("deleteTemplate unsuccessful", async () => {
//    // given
//    mockedTemplateApi.deleteTemplate.mockRejectedValue({
//      response: {
//        status: 500,
//        statusText: "Internal Server Error",
//      },
//    });
//    wrapper = shallowMount(RightsDeleteDialog, {
//      localVue: localVue,
//      mocks: { api: mockedApi },
//      pinia: createTestingPinia({
//        stubActions: false,
//      }),
//      propsData: {
//        rightId: "12",
//        index: 4,
//        metadataId: "400",
//        isTemplate: true,
//      },
//    });
//    // when
//    (wrapper.vm as any).deleteEntity();
//
//    // then
//    expect(wrapper.emitted("templateDeleteSuccessful")?.length).toBeUndefined();
//    await wrapper.vm.$nextTick();
//    // Just wait another time 'cause then it works
//    await wrapper.vm.$nextTick();
//    expect((wrapper.vm as any).deleteAlertError).toBeTruthy();
//  });
//});
