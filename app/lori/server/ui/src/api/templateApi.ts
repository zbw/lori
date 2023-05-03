import {
  Configuration,
  TemplateApi,
  TemplateIdCreated,
  TemplateRest,
} from "@/generated-sources/openapi";

const configuration = new Configuration({
  basePath: window.location.origin + "/api/v1",
});
const templateApi = new TemplateApi(configuration);

export default {
  addTemplate(template: TemplateRest): Promise<TemplateIdCreated> {
    return templateApi.addTemplate({ body: template });
  },
  deleteTemplate(templateId: string): Promise<void> {
    return templateApi.deleteTemplateById({
      id: templateId,
    });
  },
  getTemplateById(templateId: string): Promise<TemplateRest> {
    return templateApi.getTemplateById({
      id: templateId,
    });
  },
  getTemplateList(offset: number, limit: number): Promise<Array<TemplateRest>> {
    return templateApi.getTemplateList({
      offset: offset,
      limit: limit,
    });
  },
  updateTemplate(template: TemplateRest): Promise<void> {
    return templateApi.updateTemplate({ body: template });
  },
};
