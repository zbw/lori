import {
  Configuration,
  RightApi,
  RightRest,
} from "@/generated-sources/openapi";

const configuration = new Configuration({
  basePath: window.location.origin + "/api/v1",
});
const rightApi = new RightApi(configuration);

export default {
  getRightById(
      rightId: string,
      handle: string | undefined,
      ): Promise<RightRest> {
    return rightApi.getRightById({
      id: rightId,
      handle: handle,
    });
  },
};
