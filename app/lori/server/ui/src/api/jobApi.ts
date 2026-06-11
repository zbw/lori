import {
  Configuration,
  ExportFormatRest,
  ItemSearch,
  JobApi,
  JobCreatedRest,
  JobStatusUpdateRest,
} from "@/generated-sources/openapi";

const configuration = new Configuration({
  basePath: window.location.origin + "/api/v1",
});
const jobApi = new JobApi(configuration);

export default {
  createJob(
    searchTerm: string,
    format: ExportFormatRest,
  ): Promise<JobCreatedRest> {
    return jobApi.createJob({
      format: format,
      body: { searchTerm: searchTerm } as ItemSearch,
    });
  },
  getJobStatus(jobId: string): Promise<JobStatusUpdateRest> {
    return jobApi.getJobStatus({
      jobId: jobId,
    });
  },
};
