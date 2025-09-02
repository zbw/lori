import {
    Configuration, RightErrorApi, RightErrorInformationRest, RightErrorRecomputationRest, RightErrorRest, RightRest,
} from "@/generated-sources/openapi";

const configuration = new Configuration({
    basePath: window.location.origin + "/api/v1",
});
const rightErrorApi = new RightErrorApi(configuration);

export default {
    getRightErrorList(
        offset: number,
        limit: number,
        filterContext: string | undefined,
        filterTimeIntervalStart: string | undefined,
        filterTimeIntervalEnd: string | undefined,
        filterConflictType: string | undefined,
        testId: string | undefined,
    ): Promise<RightErrorInformationRest> {
        return rightErrorApi.getRightErrorList({
            pageSize: limit,
            offset: offset,
            limit: limit,
            filterContext: filterContext,
            filterTimeIntervalStart: filterTimeIntervalStart,
            filterTimeIntervalEnd: filterTimeIntervalEnd,
            filterConflictType: filterConflictType,
            testId: testId,
        });
    },

    deleteRightErrorsByTestId(
        testId: string,
    ): Promise<void> {
        return rightErrorApi.deleteErrorsByTestId({
            testId: testId
        })
    },

    recomputeRightErrors(): Promise<RightErrorRecomputationRest> {
        return rightErrorApi.recomputeRightErrors();
    }
};
