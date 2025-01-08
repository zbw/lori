import {
  AboutApi,
  AboutRest,
  Configuration,
  GroupApi,
  GroupIdCreated,
  GroupRest,
  ItemApi,
  ItemCountByRight,
  ItemEntry,
  ItemInformation,
  RightApi,
  RightIdCreated,
  RightRest,
} from "@/generated-sources/openapi";

const configuration = new Configuration({
  basePath: window.location.origin + "/api/v1",
});

const loriItem = new ItemApi(configuration);
const loriRightApi = new RightApi(configuration);
const loriGroupApi = new GroupApi(configuration);
const loriAboutApi = new AboutApi(configuration);

export default {
  /**
   * Group related calls.
   */
  addGroup(newGroup: GroupRest): Promise<GroupIdCreated> {
    return loriGroupApi.addGroup({
      body: newGroup,
    });
  },
  deleteGroup(groupId: number): Promise<void> {
    return loriGroupApi.deleteGroupById({
      id: groupId,
    });
  },
  getGroupById(groupId: number, version: number | undefined): Promise<GroupRest> {
    return loriGroupApi.getGroupById({
      id: groupId,
      version: version,
    });
  },
  getGroupList(
    offset: number,
    limit: number,
  ): Promise<Array<GroupRest>> {
    return loriGroupApi.getGroupList({
      offset: offset,
      limit: limit,
    });
  },
  updateGroup(g: GroupRest): Promise<void> {
    return loriGroupApi.updateGroup({
      body: g,
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
  addItemEntry(
    entry: ItemEntry,
    deleteRightOnConflict: boolean,
  ): Promise<void> {
    return loriItem.addItemRelation({
      body: entry,
      deleteRightOnConflict: deleteRightOnConflict,
    });
  },
  deleteRight(rightId: string): Promise<void> {
    return loriRightApi.deleteRightById({ id: rightId });
  },
  deleteItemRelation(handle: string, rightId: string): Promise<void> {
    return loriItem.deleteItem({ handle: handle, rightId: rightId });
  },
  getAboutInformation(): Promise<AboutRest> {
    return loriAboutApi.getAboutInformation();
  },
  searchQuery(
    searchTerm: string,
    offset: number,
    limit: number,
    pageSize: number,
    filterPublicationDate: string | undefined,
    filterPublicationType: string | undefined,
    filterAccessState: string | undefined,
    filterTemporalValidity: string | undefined,
    filterStartDate: string | undefined,
    filterEndDate: string | undefined,
    filterFormalRule: string | undefined,
    filterValidOn: string | undefined,
    filterPaketSigel: string | undefined,
    filterZDBId: string | undefined,
    filterNoRightInformation: string | undefined,
    filterRightId: string | undefined,
    filterSeries: string | undefined,
    filterLicenceUrl: string | undefined,
    filterManualRight: string | undefined,
  ): Promise<ItemInformation> {
    return loriItem.getSearchResult({
      searchTerm: searchTerm,
      offset: offset,
      limit: limit,
      pageSize: pageSize,
      filterPublicationDate: filterPublicationDate,
      filterPublicationType: filterPublicationType,
      filterAccessState: filterAccessState,
      filterTemporalValidity: filterTemporalValidity,
      filterStartDate: filterStartDate,
      filterEndDate: filterEndDate,
      filterFormalRule: filterFormalRule,
      filterValidOn: filterValidOn,
      filterPaketSigel: filterPaketSigel,
      filterZDBId: filterZDBId,
      filterNoRightInformation: filterNoRightInformation,
      filterRightId: filterRightId,
      filterSeries: filterSeries,
      filterLicenceUrl: filterLicenceUrl,
      filterManualRight: filterManualRight,
    });
  },
};
