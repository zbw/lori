import {
  AccessInformation,
  AccessinformationApi,
  Configuration,
} from "@/generated-sources/openapi";

const configuration = new Configuration({
  basePath: window.location.origin + "/api/v1",
});

const accessInformationApi = new AccessinformationApi(configuration);

export default {
  getList(offset: number, limit: number): Promise<Array<AccessInformation>> {
    return accessInformationApi.getAccessInformationList({
      offset: offset,
      limit: limit,
    });
  },
  deleteAccessInformation(itemId: string) {
    return accessInformationApi.deleteAccessInformationById({ id: itemId } )
  },
};
