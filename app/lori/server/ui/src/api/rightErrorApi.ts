import {
    Configuration, RightErrorApi, RightErrorRest, RightRest,
} from "@/generated-sources/openapi";

const configuration = new Configuration({
    basePath: window.location.origin + "/api/v1",
});
const rightErrorApi = new RightErrorApi(configuration);

export default {
    getRightErrorList(
        offset: number,
        limit: number,
    ): Promise<Array<RightErrorRest>> {
        return rightErrorApi.getRightErrorList({
            offset: offset,
            limit: limit,
        });
    },
};
