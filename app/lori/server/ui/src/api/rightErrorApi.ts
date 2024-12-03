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
        filterTemplateName: string | undefined,
        filterTimeIntervalStart: string | undefined,
        filterTimeIntervalEnd: string | undefined,
        filterConflictType: string | undefined,
        testId: string | undefined,
    ): Promise<RightErrorInformationRest> {
        return rightErrorApi.getRightErrorList({
            pageSize: limit,
            offset: offset,
            limit: limit,
            filterTemplateName: filterTemplateName,
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
    }
};
