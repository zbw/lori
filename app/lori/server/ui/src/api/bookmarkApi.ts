import {
  BookmarkIdCreated,
  BookmarkRawApi,
  Configuration,
} from "@/generated-sources/openapi";

const configuration = new Configuration({
  basePath: window.location.origin + "/api/v1",
});

const bookmarkRawApi = new BookmarkRawApi(configuration);

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
};
