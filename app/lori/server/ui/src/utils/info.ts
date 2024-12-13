import {TemplateApplicationRest} from "@/generated-sources/openapi";

export default {
    constructApplicationInfoText(templateApplication: TemplateApplicationRest,
    ): string {
        const parent: string =
            "Template '" +
            templateApplication.templateName +
            "' wurde für " +
            templateApplication.numberOfAppliedEntries +
            " Einträge angewandt.";
        let exceptions: string = "";
        if (templateApplication.exceptionTemplateApplications !== undefined) {
            exceptions = templateApplication.exceptionTemplateApplications
                .map(
                    (tA: TemplateApplicationRest) =>
                        "Template (Ausnahme) '" +
                        tA.templateName +
                        "' wurde für " +
                        tA.numberOfAppliedEntries +
                        " Einträge angewandt.",
                )
                .join("\n");
        }
        return parent + "\n" + exceptions;
    },
};
