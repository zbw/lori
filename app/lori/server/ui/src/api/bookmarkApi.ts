import {
  BookmarkApi,
  BookmarkIdCreated,
  BookmarkRawApi,
  BookmarkRest,
  Configuration,
} from "@/generated-sources/openapi";

const configuration = new Configuration({
  basePath: window.location.origin + "/api/v1",
});

const bookmarkRawApi = new BookmarkRawApi(configuration);
const bookmarkApi = new BookmarkApi(configuration);

export default {
  addRawBookmark(
    bookmarkName: string,
    bookmarkDescription: string | undefined,
    searchTerm: string | undefined,
    filterPublicationYear: string | undefined,
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
    filterSeries: string | undefined,
    filterTemplateName: string | undefined,
    filterLicenceUrl: string | undefined,
    filterManualRight: string | undefined,
    filterAccessonDate: string | undefined,
  ): Promise<BookmarkIdCreated> {
    return bookmarkRawApi.addBookmarkRaw({
      body: {
        bookmarkId: -1,
        bookmarkName: bookmarkName,
        description: bookmarkDescription,
        searchTerm: searchTerm,
        filterPublicationYear: filterPublicationYear,
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
        filterSeries: filterSeries,
        filterTemplateName: filterTemplateName,
        filterLicenceUrl: filterLicenceUrl,
        filterManualRight: filterManualRight,
        filterAccessOnDate: filterAccessonDate,
      },
    });
  },

  deleteBookmark(bookmarkId: number): Promise<void> {
    return bookmarkApi.deleteBookmarkById({
      id: bookmarkId,
    });
  },

  getBookmarkList(offset: number, limit: number): Promise<Array<BookmarkRest>> {
    return bookmarkApi.getBookmarkList({
      offset: offset,
      limit: limit,
    });
  },
};
