import {shallowMount, Wrapper} from "@vue/test-utils";
import {mocked} from "ts-jest/utils";
import AccessUi from "@/components/AccessUI.vue";
import Vuetify from "vuetify";
import Vue from "vue";
import api from "@/api/api";
import {ItemRest} from "@/generated-sources/openapi";

Vue.use(Vuetify);

let wrapper: Wrapper<AccessUi, Element>;
jest.mock("@/api/api");

const mockedApi = mocked(api, true);

function createWrapper() {
    return shallowMount(AccessUi, {
        propsData: {},
        mocks: {},
        stubs: {},
        methods: {},
    });
}

afterEach(() => {
    wrapper.destroy();
});

describe("Test Access UI", () => {
    it("Calls API when initialized", () => {
        mockedApi.getList.mockReturnValue(
            Promise.resolve(Array<ItemRest>())
        );
        wrapper = createWrapper();
        expect(mockedApi.getList.mock.calls).toHaveLength(1);
        expect(wrapper.text()).toMatch("AccessUI");
    });
});
