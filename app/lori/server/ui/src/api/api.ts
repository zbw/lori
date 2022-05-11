import {
  Configuration,
  ItemApi,
  ItemCountByRight,
  ItemEntry,
  ItemRest,
  RightApi,
  RightRest,
} from "@/generated-sources/openapi";

const configuration = new Configuration({
  basePath: window.location.origin + "/api/v1",
});

const loriItem = new ItemApi(configuration);
const loriRightApi = new RightApi(configuration);

export default {
  getList(offset: number, limit: number): Promise<Array<ItemRest>> {
    return loriItem.getItemList({
      offset: offset,
      limit: limit,
    });
  },
  getItemCountByRightId(rightId: string): Promise<ItemCountByRight> {
    return loriItem.getItemCountByRightId({ rightId: rightId });
  },
  updateRight(right: RightRest): Promise<void> {
    return loriRightApi.updateRight({ body: right });
  },
  addRight(right: RightRest): Promise<void> {
    return loriRightApi.addRight({ body: right });
  },
  addItemEntry(entry: ItemEntry): Promise<void> {
    return loriItem.addItemRelation({ body: entry });
  },
  deleteRight(rightId: string): Promise<void> {
    return loriRightApi.deleteRightById({ id: rightId });
  },
  deleteItemRelation(metadataId: string, rightId: string): Promise<void> {
    return loriItem.deleteItem({ metadataId: metadataId, rightId: rightId });
  },
};
