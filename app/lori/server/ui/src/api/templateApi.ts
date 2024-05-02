import {
  BookmarkRest,
  BookmarkTemplateRest,
  Configuration,
  ItemInformation,
  RightIdCreated,
  RightRest,
  TemplateApi,
  TemplateApplicationsRest,
} from "@/generated-sources/openapi";

const configuration = new Configuration({
  basePath: window.location.origin + "/api/v1",
});
const templateApi = new TemplateApi(configuration);

export default {
  addTemplate(right: RightRest): Promise<RightIdCreated> {
    return templateApi.addTemplate({ body: right });
  },
  applyTemplates(rightIds: Array<string>): Promise<TemplateApplicationsRest> {
    return templateApi.applyRightIds({
      body: {
        rightIds: rightIds,
      },
    });
  },
  deleteTemplateById(rightId: string): Promise<void> {
    return templateApi.deleteTemplateByRightId({
      id: rightId,
    });
  },
  getTemplateById(rightId: string): Promise<RightRest> {
    return templateApi.getTemplateById({
      id: rightId,
    });
  },
  getTemplateList(offset: number, limit: number): Promise<Array<RightRest>> {
    return templateApi.getTemplateList({
      offset: offset,
      limit: limit,
    });
  },
  updateTemplate(template: RightRest): Promise<void> {
    return templateApi.updateTemplate({ body: template });
  },
  getBookmarksByRightId(rightId: string): Promise<Array<BookmarkRest>> {
    return templateApi.getBookmarksByRightId({
      id: rightId,
    });
  },
  addBookmarksByRightId(
    rightId: string,
    bookmarkIds: Array<number>,
    deleteOld: boolean,
  ): Promise<Array<BookmarkTemplateRest>> {
    return templateApi.addBookmarksByRightId({
      id: rightId,
      deleteOld: deleteOld,
      body: {
        bookmarkIds: bookmarkIds,
      },
    });
  },
  getItemsByRightId(
    rightId: string,
    limit: number,
    offset: number,
  ): Promise<ItemInformation> {
    return templateApi.getItemsByRightId({
      id: rightId,
      limit: limit,
      offset: offset,
    });
  },
  addExceptionToTemplate(
    rightIdTemplate: string,
    rightIdException: Array<string>,
  ): Promise<void> {
    return templateApi.addExceptionsToTemplate({
      body: {
        idOfTemplate: rightIdTemplate,
        idsOfExceptions: rightIdException,
      },
    });
  },
  getExceptionsById(rightId: string): Promise<Array<RightRest>> {
    return templateApi.getExceptionsById({
      id: rightId,
    });
  },
};
