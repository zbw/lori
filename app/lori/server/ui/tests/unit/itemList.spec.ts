import { mount, shallowMount, Wrapper } from "@vue/test-utils";
import { mocked } from "ts-jest/utils";
import ItemList from "@/components/ItemList.vue";
import Vuetify from "vuetify";
import Vue from "vue";
import api from "@/api/api";
import {
    ItemRest,
} from "@/generated-sources/openapi";

Vue.use(Vuetify);

let wrapper: Wrapper<ItemList, Element>;
jest.mock("@/api/api");

const mockedApi = mocked(api, true);

afterEach(() => {
  wrapper.destroy();
});

describe("Test ItemList UI", () => {
  it("initial table load is successful", async () => {
    mockedApi.getList.mockReturnValue(
      Promise.resolve(
        Array<ItemRest>({
          metadata: {},
          rights: {},
        } as ItemRest)
      )
    );
    wrapper = shallowMount(ItemList, {
      mocks: { api: mockedApi },
    });
    expect((wrapper.vm as any).getAlertLoad().value).toBeFalsy();
    (wrapper.vm as any).retrieveAccessInformation();
    await wrapper.vm.$nextTick();
    expect((wrapper.vm as any).getAlertLoad().value).toBeFalsy();
  });

  it("initial table load fails", async () => {
    mockedApi.getList.mockRejectedValue({
      status: 500,
      statusText: "Internal Server Error",
    });
    wrapper = shallowMount(ItemList, {
      mocks: { api: mockedApi },
    });
    expect((wrapper.vm as any).getAlertLoad().value).toBeFalsy();
    (wrapper.vm as any).retrieveAccessInformation();
    await wrapper.vm.$nextTick();
    expect((wrapper.vm as any).getAlertLoad().value).toBeTruthy();
  });
});
