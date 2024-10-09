import {
    Configuration, RightErrorApi, RightErrorInformationRest, RightErrorRest, RightRest,
} from "@/generated-sources/openapi";

const configuration = new Configuration({
    basePath: window.location.origin + "/api/v1",
});
const rightErrorApi = new RightErrorApi(configuration);

export default {
    getRightErrorList(
        offset: number,
        limit: number,
    ): Promise<RightErrorInformationRest> {
        return rightErrorApi.getRightErrorList({
            pageSize: limit,
            offset: offset,
            limit: limit,
        });
    },
};
