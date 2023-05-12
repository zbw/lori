import {
  BookmarkApi,
  BookmarkIdCreated,
  BookmarkRawApi,
  BookmarkRest,
  Configuration,
  FilterPublicationDateRest,
  SearchKeyRest,
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
    filterNoRightInformation: string | undefined
  ): Promise<BookmarkIdCreated> {
    return bookmarkRawApi.addBookmarkRaw({
      body: {
        bookmarkId: -1,
        bookmarkName: bookmarkName,
        description: bookmarkDescription,
        searchTerm: searchTerm,
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
      },
    });
  },

  addBookmark(
    bookmarkName: string,
    bookmarkDescription: string | undefined,
    searchKeys: SearchKeyRest[] | undefined,
    filterPublicationDate: FilterPublicationDateRest | undefined,
    filterPublicationType: string[] | undefined,
    filterAccessState: string[] | undefined,
    filterTemporalValididity: string[] | undefined,
    filterStartDate: Date | undefined,
    filterEndDate: Date | undefined,
    filterFormalRule: string[] | undefined,
    filterValidOn: Date | undefined,
    filterPaketSigel: string[] | undefined,
    filterZDBId: string[] | undefined,
    filterNoRightInformation: boolean | undefined
  ): Promise<BookmarkIdCreated> {
    return bookmarkApi.addBookmark({
      body: {
        bookmarkId: -1,
        bookmarkName: bookmarkName,
        description: bookmarkDescription,
        searchKeys: searchKeys,
        filterPublicationDate: filterPublicationDate,
        filterPublicationType: filterPublicationType,
        filterAccessState: filterAccessState,
        filterTemporalValidity: filterTemporalValididity,
        filterStartDate: filterStartDate,
        filterEndDate: filterEndDate,
        filterFormalRule: filterFormalRule,
        filterValidOn: filterValidOn,
        filterPaketSigel: filterPaketSigel,
        filterZDBId: filterZDBId,
        filterNoRightInformation: filterNoRightInformation,
      },
    });
  },

  deleteBookmark(bookmarkId: number): Promise<void> {
    return bookmarkApi.deleteBookmarkById({
      id: bookmarkId,
    });
  },

  updateBookmark(
    bookmarkId: number,
    bookmarkName: string,
    bookmarkDescription: string | undefined,
    searchKeys: SearchKeyRest[] | undefined,
    filterPublicationDate: FilterPublicationDateRest | undefined,
    filterPublicationType: string[] | undefined,
    filterAccessState: string[] | undefined,
    filterTemporalValididity: string[] | undefined,
    filterStartDate: Date | undefined,
    filterEndDate: Date | undefined,
    filterFormalRule: string[] | undefined,
    filterValidOn: Date | undefined,
    filterPaketSigel: string[] | undefined,
    filterZDBId: string[] | undefined,
    filterNoRightInformation: boolean | undefined
  ): Promise<void> {
    return bookmarkApi.updateBookmark({
      body: {
        bookmarkId: bookmarkId,
        bookmarkName: bookmarkName,
        description: bookmarkDescription,
        searchKeys: searchKeys,
        filterPublicationDate: filterPublicationDate,
        filterPublicationType: filterPublicationType,
        filterAccessState: filterAccessState,
        filterTemporalValidity: filterTemporalValididity,
        filterStartDate: filterStartDate,
        filterEndDate: filterEndDate,
        filterFormalRule: filterFormalRule,
        filterValidOn: filterValidOn,
        filterPaketSigel: filterPaketSigel,
        filterZDBId: filterZDBId,
        filterNoRightInformation: filterNoRightInformation,
      },
    });
  },

  getBookmarkList(offset: number, limit: number): Promise<Array<BookmarkRest>> {
    return bookmarkApi.getBookmarkList({
      offset: offset,
      limit: limit,
    });
  },
};
