import {
  BookmarkTemplateRest,
  BookmarktemplatesApi,
  Configuration,
} from "@/generated-sources/openapi";

const configuration = new Configuration({
  basePath: window.location.origin + "/api/v1",
});
const bookmarkTemplateApi = new BookmarktemplatesApi(configuration);

export default {
  deletePair(rightId: string, bookmarkId: number): Promise<void> {
    return bookmarkTemplateApi.deleteBookmarkTemplate({
      bookmarkId: bookmarkId,
      rightId: rightId,
    });
  },
  addPair(rightId: string, bookmarkId: number): Promise<void> {
    return bookmarkTemplateApi.addBookmarkTemplate({
      body: {
        bookmarkId: bookmarkId,
        rightId: rightId,
      },
    });
  },
  addBookmarkTemplateBatch(
    batch: Array<BookmarkTemplateRest>,
  ): Promise<Array<BookmarkTemplateRest>> {
    return bookmarkTemplateApi.addBookmarkTemplateBatch({
      body: {
        batch: batch,
      },
    });
  },
};
