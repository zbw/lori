import { TemplateApplicationRest } from "@/generated-sources/openapi";

export default {
  constructApplicationInfoText(
    templateApplication: TemplateApplicationRest,
  ): string {
    const parent: string =
      "Template '" +
      templateApplication.templateName +
      "' wurde für " +
      templateApplication.numberOfAppliedEntries +
      " Einträge angewandt.";
    let exception: string = "";
    if (templateApplication.exceptionTemplateApplication !== undefined) {
      exception =
        "Template (Ausnahme) '" +
        templateApplication.exceptionTemplateApplication.templateName +
        "' wurde für " +
        templateApplication.exceptionTemplateApplication
          .numberOfAppliedEntries +
        " Einträge angewandt.";
    }
    return parent + "\n" + exception;
  },
};
