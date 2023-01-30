import { createLocalVue, shallowMount, Wrapper } from "@vue/test-utils";
import { mocked } from "ts-jest/utils";
import Vuetify from "vuetify";
import Vue from "vue";
import api from "@/api/api";
import RightsEditDialog from "@/components/RightsEditDialog.vue";
import { PiniaVuePlugin } from "pinia";
import { createTestingPinia } from "@pinia/testing";
import { ChangeType, HistoryEntry, useHistoryStore } from "@/stores/history";
import {
  RightIdCreated,
  RightRest,
  AccessStateRest,
  GroupRest,
} from "@/generated-sources/openapi";

const localVue = createLocalVue();
localVue.use(PiniaVuePlugin);

Vue.use(Vuetify);

let wrapper: Wrapper<RightsEditDialog, Element>;
jest.mock("@/api/api");

const mockedApi = mocked(api, true);

afterEach(() => {
  jest.clearAllMocks();
  wrapper.destroy();
});

describe("Test RightsEditDialog", () => {
  it("updateRight successful", async () => {
    // given
    const givenIndex = 5;
    const givenRightId = "123";
    const givenRight = {
      rightId: givenRightId,
      accessState: AccessStateRest.Open,
      startDate: new Date("2022-12-12"),
      endDate: undefined,
    } as RightRest;
    const expectedHistoryEntry = {
      type: ChangeType.UPDATED,
      rightId: givenRightId,
    };
    mockedApi.updateRight.mockReturnValue(Promise.resolve());
    mockedApi.getGroupList.mockReturnValue(
      Promise.resolve([
        {
          name: "foo",
        },
      ] as Array<GroupRest>)
    );
    wrapper = shallowMount(RightsEditDialog, {
      localVue: localVue,
      mocks: { api: mockedApi },
      pinia: createTestingPinia({
        stubActions: false,
      }),
      propsData: {
        right: givenRight,
        index: givenIndex,
        metadataId: "400",
        isNew: false,
      },
    });
    const historyStore = useHistoryStore();
    expect(historyStore.numberEntries).toBe(0);

    // when
    (wrapper.vm as any).updateRight();
    await wrapper.vm.$nextTick();

    // then
    expect(historyStore.history).toStrictEqual([
      expectedHistoryEntry,
    ] as HistoryEntry[]);
    expect(historyStore.numberEntries).toBe(1);
    // Verify emit of event
    expect(wrapper.emitted("updateSuccessful")?.at(0)[1]).toBe(givenIndex);
    expect(wrapper.emitted("updateSuccessful")?.at(0)[0]).toStrictEqual(
      givenRight
    );
  });

  it("updateRight unsuccessful", async () => {
    // given
    mockedApi.updateRight.mockRejectedValue({
      response: {
        status: 500,
        statusText: "Internal Server Error",
      },
    });
    mockedApi.getGroupList.mockReturnValue(
      Promise.resolve([
        {
          name: "foo",
        },
      ] as Array<GroupRest>)
    );
    wrapper = shallowMount(RightsEditDialog, {
      localVue: localVue,
      mocks: { api: mockedApi },
      pinia: createTestingPinia({
        stubActions: false,
      }),
      propsData: {
        right: {
          rightId: "12",
          accessState: AccessStateRest.Open,
          startDate: new Date(2022, 12, 12),
        } as RightRest,
        index: 4,
        metadataId: "400",
      },
    });
    const historyStore = useHistoryStore();
    expect(historyStore.numberEntries).toBe(0);

    // when
    (wrapper.vm as any).updateRight();

    // then
    expect(historyStore.numberEntries).toBe(0);
    expect(wrapper.emitted("updateSuccessful")?.length).toBeUndefined();
    await wrapper.vm.$nextTick();
    // Just wait another time 'cause then it works
    await wrapper.vm.$nextTick();
    expect((wrapper.vm as any).saveAlertError).toBeTruthy();
  });

  it("createRight successful", async () => {
    // given
    const givenIndex = 5;
    const givenRightId = "123";
    const givenRight = {
      rightId: givenRightId,
      accessState: AccessStateRest.Open,
      startDate: new Date("2022-12-12"),
      endDate: undefined,
    } as RightRest;
    const expectedHistoryEntry = {
      type: ChangeType.CREATED,
      rightId: givenRightId,
    };
    mockedApi.addRight.mockReturnValue(
      Promise.resolve({
        rightId: givenRightId,
      } as RightIdCreated)
    );
    mockedApi.getGroupList.mockReturnValue(
      Promise.resolve([
        {
          name: "foo",
        },
      ] as Array<GroupRest>)
    );
    mockedApi.addItemEntry.mockReturnValue(Promise.resolve());
    wrapper = shallowMount(RightsEditDialog, {
      localVue: localVue,
      mocks: { api: mockedApi },
      pinia: createTestingPinia({
        stubActions: false,
      }),
      propsData: {
        right: givenRight,
        index: givenIndex,
        metadataId: "400",
        isNew: false,
      },
    });
    const historyStore = useHistoryStore();
    expect(historyStore.numberEntries).toBe(0);

    // when
    (wrapper.vm as any).createRight();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    // then
    // Verify emit of event
    expect(historyStore.history).toStrictEqual([
      expectedHistoryEntry,
    ] as HistoryEntry[]);
    expect(historyStore.numberEntries).toBe(1);
    expect(wrapper.emitted("addSuccessful")?.at(0)[0]).toStrictEqual(
      givenRight
    );
  });

  it("createRight unsuccessful - addRight", async () => {
    // given
    const givenIndex = 5;
    const givenRightId = "123";
    const givenRight = {
      rightId: givenRightId,
      accessState: AccessStateRest.Open,
      startDate: new Date("2022-12-12"),
      endDate: undefined,
    } as RightRest;

    mockedApi.addRight.mockRejectedValue({
      response: {
        status: 500,
        statusText: "Internal Server Error",
      },
    });
    mockedApi.getGroupList.mockReturnValue(
      Promise.resolve([
        {
          name: "foo",
        },
      ] as Array<GroupRest>)
    );
    mockedApi.addItemEntry.mockReturnValue(Promise.resolve());
    wrapper = shallowMount(RightsEditDialog, {
      localVue: localVue,
      mocks: { api: mockedApi },
      pinia: createTestingPinia({
        stubActions: false,
      }),
      propsData: {
        right: givenRight,
        index: givenIndex,
        metadataId: "400",
        isNew: false,
      },
    });
    const historyStore = useHistoryStore();
    expect(historyStore.numberEntries).toBe(0);

    // when
    (wrapper.vm as any).createRight();
    await wrapper.vm.$nextTick();

    // then
    // Verify emit of event
    expect(historyStore.numberEntries).toBe(0);
    expect(wrapper.emitted("addSuccessful")?.length).toBeUndefined();
    expect(mockedApi.addRight).toBeCalledTimes(1);
    expect(mockedApi.addItemEntry).toBeCalledTimes(0);
  });

  it("createRight unsuccessful - addItemEntry", async () => {
    // given
    const givenIndex = 5;
    const givenRightId = "123";
    const givenRight = {
      rightId: givenRightId,
      accessState: AccessStateRest.Open,
      startDate: new Date("2022-12-12"),
      endDate: undefined,
    } as RightRest;

    mockedApi.addRight.mockReturnValue(
      Promise.resolve({
        rightId: givenRightId,
      } as RightIdCreated)
    );
    mockedApi.getGroupList.mockReturnValue(
      Promise.resolve([
        {
          name: "foo",
        },
      ] as Array<GroupRest>)
    );
    mockedApi.addItemEntry.mockRejectedValue({
      response: {
        status: 500,
        statusText: "Internal Server Error",
      },
    });
    wrapper = shallowMount(RightsEditDialog, {
      localVue: localVue,
      mocks: { api: mockedApi },
      pinia: createTestingPinia({
        stubActions: false,
      }),
      propsData: {
        right: givenRight,
        index: givenIndex,
        metadataId: "400",
        isNew: false,
      },
    });
    const historyStore = useHistoryStore();
    expect(historyStore.numberEntries).toBe(0);

    // when
    (wrapper.vm as any).createRight();
    await wrapper.vm.$nextTick();

    // then
    // Verify emit of event
    expect(historyStore.numberEntries).toBe(0);
    expect(wrapper.emitted("addSuccessful")?.length).toBeUndefined();
    expect(mockedApi.addRight).toBeCalledTimes(1);
    expect(mockedApi.addItemEntry).toBeCalledTimes(1);
  });

  it("vuelidate form", async () => {
    // given
    const givenIndex = 5;
    const givenRightId = "123";
    const givenRight = {
      rightId: givenRightId,
      accessState: AccessStateRest.Open,
      startDate: new Date("2022-12-12"),
      endDate: undefined,
    } as RightRest;

    wrapper = shallowMount(RightsEditDialog, {
      localVue: localVue,
      mocks: { api: mockedApi },
      pinia: createTestingPinia({
        stubActions: false,
      }),
      propsData: {
        right: givenRight,
        index: givenIndex,
        metadataId: "400",
        isNew: false,
      },
    });
    // when
    (wrapper.vm as any).formState.accessState = "";
    (wrapper.vm as any).formState.startDate = "";
    (wrapper.vm as any).v$.$touch();
    // then
    await wrapper.vm.$nextTick();
    expect((wrapper.vm as any).v$.startDate.required.$invalid).toBeTruthy();
    expect((wrapper.vm as any).v$.accessState.required.$invalid).toBeTruthy();

    // when
    (wrapper.vm as any).formState.accessState = "Open";
    (wrapper.vm as any).formState.startDate = "2022-12-12";
    (wrapper.vm as any).formState.endDate = "2022-12-01";
    (wrapper.vm as any).v$.$touch();

    // then
    await wrapper.vm.$nextTick();

    // then
    expect((wrapper.vm as any).v$.endDate.$invalid).toBeTruthy();
    expect((wrapper.vm as any).v$.startDate.required.$invalid).toBeFalsy();
    expect((wrapper.vm as any).v$.accessState.required.$invalid).toBeFalsy();
  });

  it("save validation update", async () => {
    // given
    const givenIndex = 5;
    const givenRightId = "123";
    const givenRight = {
      rightId: givenRightId,
      accessState: AccessStateRest.Open,
      startDate: new Date("2022-12-12"),
      endDate: undefined,
    } as RightRest;
    mockedApi.updateRight.mockRejectedValue({
      response: {
        status: 500,
        statusText: "Internal Server Error",
      },
    });
    mockedApi.getGroupList.mockReturnValue(
      Promise.resolve([
        {
          name: "foo",
        },
      ] as Array<GroupRest>)
    );
    wrapper = shallowMount(RightsEditDialog, {
      localVue: localVue,
      mocks: { api: mockedApi },
      pinia: createTestingPinia({
        stubActions: false,
      }),
      propsData: {
        right: givenRight,
        index: givenIndex,
        metadataId: "400",
        isNew: false,
      },
    });
    // when
    (wrapper.vm as any).formState.accessState = "Open";
    (wrapper.vm as any).formState.startDate = "2022-12-12";
    (wrapper.vm as any).formState.endDate = "2022-12-13";
    (wrapper.vm as any).v$.$touch();

    // then
    await wrapper.vm.$nextTick();

    // then
    expect((wrapper.vm as any).v$.endDate.$invalid).toBeFalsy();
    expect((wrapper.vm as any).v$.startDate.required.$invalid).toBeFalsy();
    expect((wrapper.vm as any).v$.accessState.required.$invalid).toBeFalsy();

    // when
    await (wrapper.vm as any).save();
    expect(mockedApi.updateRight).toBeCalledTimes(1);
  });

  it("save validation create", async () => {
    // given
    const givenIndex = 5;
    const givenRightId = "123";
    const givenRight = {
      rightId: givenRightId,
      accessState: AccessStateRest.Open,
      startDate: new Date("2022-12-12"),
      endDate: undefined,
    } as RightRest;
    mockedApi.addRight.mockRejectedValue({
      response: {
        status: 500,
        statusText: "Internal Server Error",
      },
    });
    mockedApi.getGroupList.mockReturnValue(
      Promise.resolve([
        {
          name: "foo",
        },
      ] as Array<GroupRest>)
    );
    wrapper = shallowMount(RightsEditDialog, {
      localVue: localVue,
      mocks: { api: mockedApi },
      pinia: createTestingPinia({
        stubActions: false,
      }),
      propsData: {
        right: givenRight,
        index: givenIndex,
        metadataId: "400",
        isNew: true,
      },
    });
    // when
    (wrapper.vm as any).formState.accessState = "Open";
    (wrapper.vm as any).formState.startDate = "2022-12-12";
    (wrapper.vm as any).formState.endDate = "2022-12-13";
    (wrapper.vm as any).v$.$touch();

    // then
    await wrapper.vm.$nextTick();

    // then
    expect((wrapper.vm as any).v$.endDate.$invalid).toBeFalsy();
    expect((wrapper.vm as any).v$.startDate.required.$invalid).toBeFalsy();
    expect((wrapper.vm as any).v$.accessState.required.$invalid).toBeFalsy();

    // when
    await (wrapper.vm as any).save();
    expect(mockedApi.addRight).toBeCalledTimes(1);
  });
});
