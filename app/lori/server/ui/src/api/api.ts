import {
  Configuration,
  ItemCountByRight,
  ItemRelationApi,
  ItemObjectListApi,
  ItemRest,
  RightApi,
  RightRest,
} from "@/generated-sources/openapi";

const configuration = new Configuration({
  basePath: window.location.origin + "/api/v1",
});

const loriItemListApi = new ItemObjectListApi(configuration);
const loriItemRelationApi = new ItemRelationApi(configuration);
const loriRightApi = new RightApi(configuration);

export default {
  getList(offset: number, limit: number): Promise<Array<ItemRest>> {
    return loriItemListApi.getItemList({
      offset: offset,
      limit: limit,
    });
  },
  getItemCountByRightId(rightId: string): Promise<ItemCountByRight> {
    return loriItemRelationApi.getItemCountByRightId({ rightId: rightId });
  },
  updateRight(right: RightRest): Promise<void> {
    return loriRightApi.updateRight({ body: right });
  },
};
