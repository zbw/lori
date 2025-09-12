import {Configuration, ItemSearch, JobApi, JobCreatedRest, RightRest} from "@/generated-sources/openapi";

const configuration = new Configuration({
    basePath: window.location.origin + "/api/v1",
});
const jobApi = new JobApi(configuration);

export default {
    createJob(
        searchTerm: string,
    ): Promise<JobCreatedRest> {
        return jobApi.createJob({
            body: { searchTerm: searchTerm} as ItemSearch,
        });
    },
};
