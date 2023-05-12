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
  deletePair(templateId: number, bookmarkId: number): Promise<void> {
    return bookmarkTemplateApi.deleteBookmarkTemplate({
      bookmarkId: bookmarkId,
      templateId: templateId,
    });
  },
  addPair(templateId: number, bookmarkId: number): Promise<void> {
    return bookmarkTemplateApi.addBookmarkTemplate({
      body: {
        bookmarkId: bookmarkId,
        templateId: templateId,
      },
    });
  },
  addBookmarkTemplateBatch(
    batch: Array<BookmarkTemplateRest>
  ): Promise<Array<BookmarkTemplateRest>> {
    return bookmarkTemplateApi.addBookmarkTemplateBatch({
      body: {
        batch: batch,
      },
    });
  },
};
