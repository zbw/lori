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
  getItemById(id: string): Promise<AccessInformation> {
    return accessInformationApi.getAccessInformationByIds({
      id: id,
    });
  },
  getItemByIds(ids: Array<string>): Promise<AccessInformation> {
    return accessInformationApi.getAccessInformationByIds({
      id: ids.join(","),
    });
  },
  deleteAccessInformation(itemId: string): Promise<void> {
    return accessInformationApi.deleteAccessInformationById({ id: itemId });
  },
};
