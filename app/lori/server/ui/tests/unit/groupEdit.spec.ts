import { createLocalVue, shallowMount, Wrapper } from "@vue/test-utils";
import { mocked } from "ts-jest/utils";
import Vuetify from "vuetify";
import Vue from "vue";
import api from "@/api/api";
import GroupEdit from "@/components/GroupEdit.vue";
import { PiniaVuePlugin } from "pinia";
import { createTestingPinia } from "@pinia/testing";
import { GroupIdCreated, GroupRest } from "@/generated-sources/openapi";

const localVue = createLocalVue();
localVue.use(PiniaVuePlugin);
Vue.use(Vuetify);

let wrapper: Wrapper<GroupEdit, Element>;
jest.mock("@/api/api");
const mockedApi = mocked(api, true);

afterEach(() => {
  jest.clearAllMocks();
  wrapper.destroy();
});

describe("Test GroupEdit", () => {
  it("createGroup successful", async () => {
    const givenGroup = {
      name: "foo",
      description: "bla",
      ipAddresses: "someOrga,localhost",
    } as GroupRest;

    const givenGroupId = "foo";

    mockedApi.addGroup.mockReturnValue(
      Promise.resolve({
        groupId: givenGroupId,
      } as GroupIdCreated)
    );
    wrapper = shallowMount(GroupEdit, {
      localVue: localVue,
      mocks: { api: mockedApi },
      pinia: createTestingPinia({
        stubActions: false,
      }),
      propsData: {
        group: givenGroup,
        isNew: true,
      },
    });
    // when
    (wrapper.vm as any).groupTmp = givenGroup;
    (wrapper.vm as any).createGroup();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    expect(wrapper.emitted("addGroupSuccessful")?.at(0)[0]).toStrictEqual(
      givenGroup.name
    );
  });

  it("createGroup unsuccessful", async () => {
    const givenGroup = {
      name: "foo",
      description: "bla",
      ipAddresses: "someOrga,localhost",
    } as GroupRest;

    mockedApi.addGroup.mockRejectedValue({
      response: {
        response: {
          status: 500,
          statusText: "Internal Server Error",
        },
      },
    });
    wrapper = shallowMount(GroupEdit, {
      localVue: localVue,
      mocks: { api: mockedApi },
      pinia: createTestingPinia({
        stubActions: false,
      }),
      propsData: {
        group: givenGroup,
        isNew: true,
      },
    });
    // when
    (wrapper.vm as any).createGroup();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    expect(wrapper.emitted("addGroupSuccessful")?.length).toBeUndefined();
    expect((wrapper.vm as any).saveAlertError).toBeTruthy();
  });

  it("updateGroup successful", async () => {
    const givenGroup = {
      name: "foo",
      description: "bla",
      ipAddresses: "someOrga,localhost",
    } as GroupRest;

    const givenGroupId = "foo";

    mockedApi.updateGroup.mockReturnValue(Promise.resolve());
    wrapper = shallowMount(GroupEdit, {
      localVue: localVue,
      mocks: { api: mockedApi },
      pinia: createTestingPinia({
        stubActions: false,
      }),
      propsData: {
        group: givenGroup,
        isNew: true,
      },
    });
    // when
    (wrapper.vm as any).groupTmp = givenGroup;
    (wrapper.vm as any).updateGroup();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    expect(wrapper.emitted("updateGroupSuccessful")?.at(0)[0]).toStrictEqual(
      givenGroup.name
    );
  });

  it("updateGroup unsuccessful", async () => {
    const givenGroup = {
      name: "foo",
      description: "bla",
      ipAddresses: "someOrga,localhost",
    } as GroupRest;

    mockedApi.updateGroup.mockRejectedValue({
      response: {
        status: 500,
        statusText: "Internal Server Error",
      },
    });
    wrapper = shallowMount(GroupEdit, {
      localVue: localVue,
      mocks: { api: mockedApi },
      pinia: createTestingPinia({
        stubActions: false,
      }),
      propsData: {
        group: givenGroup,
        isNew: true,
      },
    });
    // when
    (wrapper.vm as any).updateGroup();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    expect(wrapper.emitted("updateGroupSuccessful")?.length).toBeUndefined();
    expect((wrapper.vm as any).saveAlertError).toBeTruthy();
  });
});
