import {
  Configuration,
  ItemApi,
  ItemCountByRight,
  ItemEntry,
  ItemInformation,
  RightApi,
  RightIdCreated,
  RightRest,
} from "../generated-sources/openapi";

const configuration = new Configuration({
  basePath: window.location.origin + "/api/v1",
});

const loriItem = new ItemApi(configuration);
const loriRightApi = new RightApi(configuration);

export default {
  getList(
    offset: number,
    limit: number,
    pageSize: number
  ): Promise<ItemInformation> {
    return loriItem.getItemList({
      offset: offset,
      limit: limit,
      pageSize: pageSize,
    });
  },
  getItemCountByRightId(rightId: string): Promise<ItemCountByRight> {
    return loriItem.getItemCountByRightId({ rightId: rightId });
  },
  updateRight(right: RightRest): Promise<void> {
    return loriRightApi.updateRight({ body: right });
  },
  addRight(right: RightRest): Promise<RightIdCreated> {
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
  searchQuery(
    searchTerm: string,
    offset: number,
    limit: number,
    pageSize: number,
    filterPublicationDate: string | undefined,
    filterPublicationType: string | undefined,
    filterAccessState: string | undefined,
    filterTemporalValidity: string | undefined
  ): Promise<ItemInformation> {
    return loriItem.getSearchResult({
      searchTerm: searchTerm,
      offset: offset,
      limit: limit,
      pageSize: pageSize,
      filterPublicationDate: filterPublicationDate,
      filterPublicationType: filterPublicationType,
      filterAccessState: filterAccessState,
      filterTemporalValidity: filterTemporalValidity
    });
  },
};
