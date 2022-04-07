import {
  Configuration,
  ItemObjectListApi,
  ItemRest,
} from "@/generated-sources/openapi";

const configuration = new Configuration({
  basePath: window.location.origin + "/api/v1",
});

const accessInformationApi = new ItemObjectListApi(configuration);

export default {
  getList(offset: number, limit: number): Promise<Array<ItemRest>> {
    return accessInformationApi.getItemList({
      offset: offset,
      limit: limit,
    });
  },
};
