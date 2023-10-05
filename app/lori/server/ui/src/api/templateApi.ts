import {
  BookmarkRest,
  BookmarkTemplateRest,
  Configuration,
  ItemInformation,
  RightRest,
  TemplateApi,
  TemplateApplicationsRest,
  TemplateIdCreated,
} from "@/generated-sources/openapi";

const configuration = new Configuration({
  basePath: window.location.origin + "/api/v1",
});
const templateApi = new TemplateApi(configuration);

export default {
  addTemplate(right: RightRest): Promise<TemplateIdCreated> {
    return templateApi.addTemplate({ body: right });
  },
  applyTemplates(
    templateIds: Array<number>
  ): Promise<TemplateApplicationsRest> {
    return templateApi.applyTemplateIds({
      body: {
        templateIds: templateIds,
      },
    });
  },
  deleteTemplate(templateId: number): Promise<void> {
    return templateApi.deleteTemplateById({
      id: templateId,
    });
  },
  getTemplateById(templateId: string): Promise<RightRest> {
    return templateApi.getTemplateById({
      id: templateId,
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
  getBookmarksByTemplateId(templateId: number): Promise<Array<BookmarkRest>> {
    return templateApi.getBookmarksByTemplateId({
      id: templateId,
    });
  },
  addBookmarksByTemplateId(
    templateId: number,
    bookmarkIds: Array<number>,
    deleteOld: boolean
  ): Promise<Array<BookmarkTemplateRest>> {
    return templateApi.addBookmarksByTemplateId({
      id: templateId,
      deleteOld: deleteOld,
      body: {
        bookmarkIds: bookmarkIds,
      },
    });
  },
  getItemsByTemplateId(
    templateId: number,
    limit: number,
    offset: number
  ): Promise<ItemInformation> {
    return templateApi.getItemsByTemplateId({
      id: templateId,
      limit: limit,
      offset: offset,
    });
  },
};
