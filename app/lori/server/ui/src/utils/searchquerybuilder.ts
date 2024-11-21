import {
  AccessStateRest,
  AccessStateWithCountRest,
  BookmarkRest, IsPartOfSeriesCountRest, LicenceUrlCountRest,
  PaketSigelWithCountRest,
  PublicationTypeRest,
  PublicationTypeWithCountRest,
  TemplateNameWithCountRest,
  ZdbIdWithCountRest,
} from "@/generated-sources/openapi";

const QUERY_PARAMETER_TEMPLATE_ID = "templateId";
const QUERY_PARAMETER_RIGHT_ID = "rightId";
const QUERY_PARAMETER_HANDLE = "handle";
const QUERY_PARAMETER_DASHBOARD_HANDLE_SEARCH = "dashboardHandleSearch";
export default {
  QUERY_PARAMETER_RIGHT_ID,
  QUERY_PARAMETER_HANDLE,
  QUERY_PARAMETER_DASHBOARD_HANDLE_SEARCH,
  QUERY_PARAMETER_TEMPLATE_ID,

  setPublicationDateFilter(searchStore: any, bookmark: BookmarkRest): void {
    if (bookmark.filterPublicationDate == undefined) {
      searchStore.publicationDateFrom = "";
      searchStore.publicationDateTo = "";
      return;
    }
    if (bookmark.filterPublicationDate.fromYear !== undefined) {
      searchStore.publicationDateFrom = bookmark.filterPublicationDate.fromYear;
    }
    if (bookmark.filterPublicationDate.toYear !== undefined) {
      searchStore.publicationDateTo = bookmark.filterPublicationDate.toYear;
    }
  },

  buildPublicationDateFilter(searchStore: any): string | undefined {
    return searchStore.publicationDateFrom == "" &&
      searchStore.publicationDateTo == ""
      ? undefined
      : searchStore.publicationDateFrom + "-" + searchStore.publicationDateTo;
  },

  setPaketSigelFilter(searchStore: any, bookmark: BookmarkRest): void {
    if (
      bookmark.filterPaketSigel == undefined ||
      bookmark.filterPaketSigel.length == 0
    ) {
      searchStore.paketSigelIdIdx = [];
      return;
    }
    searchStore.paketSigelIdIdx = Array(bookmark.filterPaketSigel.length).fill(
      true,
    );
    searchStore.paketSigelIdReceived = Array(bookmark.filterPaketSigel.length);
    bookmark.filterPaketSigel.forEach((v: string, index: number): void => {
      searchStore.paketSigelIdReceived[index] = {
        count: 0,
        paketSigel: v,
      } as PaketSigelWithCountRest;
    });
  },

  buildPaketSigelIdFilter(searchStore: any): string | undefined {
    const paketSigelIds: Array<string> = [];
    searchStore.paketSigelIdIdx.forEach(
      (i: boolean | undefined, index: number): void => {
        if (i) {
          paketSigelIds.push(
            searchStore.paketSigelIdReceived[index].paketSigel,
          );
        }
      },
    );
    // Remind selected ids, for resetting the filter afterwards correctly.
    searchStore.paketSigelSelectedLastSearch = paketSigelIds;
    if (paketSigelIds.length == 0) {
      return undefined;
    } else {
      return paketSigelIds.join(",");
    }
  },

  setZDBFilter(searchStore: any, bookmark: BookmarkRest): void {
    if (bookmark.filterZDBId == undefined || bookmark.filterZDBId.length == 0) {
      searchStore.zdbIdIdx = [];
      return;
    }
    searchStore.zdbIdIdx = Array(bookmark.filterZDBId.length).fill(true);
    searchStore.zdbIdReceived = Array(bookmark.filterZDBId.length);
    bookmark.filterZDBId.forEach((v: string, index: number): void => {
      searchStore.zdbIdReceived[index] = {
        count: 0,
        zdbId: v,
      } as ZdbIdWithCountRest;
    });
  },

  buildZDBIdFilter(searchStore: any): string | undefined {
    const zdbIds: Array<string> = [];
    searchStore.zdbIdIdx.forEach(
      (i: boolean | undefined, index: number): void => {
        if (i) {
          zdbIds.push(searchStore.zdbIdReceived[index].zdbId);
        }
      },
    );
    // Remind selected ids, for resetting the filter afterwards correctly.
    searchStore.zdbIdSelectedLastSearch = zdbIds;
    if (zdbIds.length == 0) {
      return undefined;
    } else {
      return zdbIds.join(",");
    }
  },

  setSeriesFilter(searchStore: any, bookmark: BookmarkRest): void {
    if (bookmark.filterSeries == undefined || bookmark.filterSeries.length == 0) {
      searchStore.seriesIdx = [];
      return;
    }
    searchStore.seriesIdx = Array(bookmark.filterSeries.length).fill(true);
    searchStore.seriesReceived = Array(bookmark.filterSeries.length);
    bookmark.filterSeries.forEach((v: string, index: number): void => {
      searchStore.seriesReceived[index] = {
        count: 0,
        series: v,
      } as IsPartOfSeriesCountRest;
    });
  },

  buildSeriesFilter(searchStore: any): string | undefined {
    const seriesIds: Array<string> = [];
    searchStore.seriesIdx.forEach(
        (i: boolean | undefined, index: number): void => {
          if (i) {
            seriesIds.push(searchStore.seriesReceived[index].series);
          }
        },
    );
    // Remind selected ids, for resetting the filter afterward correctly.
    searchStore.seriesSelectedLastSearch = seriesIds;
    if (seriesIds.length == 0) {
      return undefined;
    } else {
      return seriesIds.join(",");
    }
  },

  setLicenceUrlFilter(searchStore: any, bookmark: BookmarkRest): void {
    if (bookmark.filterLicenceUrl == undefined || bookmark.filterLicenceUrl.length == 0) {
      searchStore.licenceUrlIdx = [];
      return;
    }
    searchStore.licenceUrlIdx = Array(1).fill(true);
    searchStore.licenceUrlReceived = Array(1);
    searchStore.licenceUrlReceived[0] = {
        count: 0,
        licenceUrl: bookmark.filterLicenceUrl,
    } as LicenceUrlCountRest;
  },

  buildLicenceUrlFilter(searchStore: any): string | undefined {
    const licenceUrls: Array<string> = [];
    searchStore.licenceUrlIdx.forEach(
        (i: boolean | undefined, index: number): void => {
          if (i) {
            licenceUrls.push(searchStore.licenceUrlReceived[index].licenceUrl);
          }
        },
    );
    // Remind selected ids, for resetting the filter afterward correctly.
    searchStore.licenceUrlSelectedLastSearch = licenceUrls;
    if (licenceUrls.length == 0) {
      return undefined;
    } else {
      return licenceUrls.join(",");
    }
  },


  setTemplateNameFilter(searchStore: any, bookmark: BookmarkRest): void {
    if (
      bookmark.filterTemplateName == undefined ||
      bookmark.filterTemplateName.length == 0
    ) {
      searchStore.templateNameIdx = [];
      return;
    }
    searchStore.templateNameIdx = Array(bookmark.filterTemplateName.length).fill(true);
    searchStore.templateNameReceived = Array(bookmark.filterTemplateName.length);
    bookmark.filterTemplateName.forEach((v: string, index: number): void => {
      searchStore.templateNameReceived[index] = {
        count: 0,
        templateName: v,
      } as TemplateNameWithCountRest;
    });
  },

  buildTemplateNameFilter(searchStore: any): string | undefined {
    const rightIds: Array<string> = [];
    searchStore.templateNameIdx.forEach(
      (i: boolean | undefined, index: number): void => {
        if (i) {
          rightIds.push(searchStore.templateNameReceived[index].templateName);
        }
      },
    );
    // Remind selected ids, for resetting the filter afterwards correctly.
    searchStore.templateNameSelectedLastSearch = rightIds;
    if (rightIds.length == 0) {
      return undefined;
    } else {
      return rightIds.join(",");
    }
  },

  setAccessStateFilter(searchStore: any, bookmark: BookmarkRest): void {
    if (
      bookmark.filterAccessState == undefined ||
      bookmark.filterAccessState.length == 0
    ) {
      searchStore.accessStateIdx = [];
      return;
    }
    searchStore.accessStateIdx = Array(bookmark.filterAccessState.length).fill(
      true,
    );
    searchStore.accessStateReceived = Array(bookmark.filterAccessState.length);
    bookmark.filterAccessState.forEach((v: string, index: number): void => {
      searchStore.accessStateReceived[index] = {
        count: 0,
        accessState: v,
      } as AccessStateWithCountRest;
    });
  },

  buildAccessStateFilter(searchStore: any): string | undefined {
    const accessStates: Array<string> = [];
    searchStore.accessStateIdx.forEach(
      (i: boolean | undefined, index: number): void => {
        if (i) {
          accessStates.push(
            searchStore.accessStateReceived[index].accessState.toUpperCase(),
          );
        }
      },
    );

    // Remind selected ids, for resetting the filter afterwards correctly.
    searchStore.accessStateSelectedLastSearch = accessStates.map((value) =>
      value.toLowerCase(),
    );
    if (accessStates.length == 0) {
      return undefined;
    } else {
      return accessStates.join(",");
    }
  },

  setFormalRuleFilter(searchStore: any, bookmark: BookmarkRest): void {
    if (
      bookmark.filterFormalRule == undefined ||
      bookmark.filterFormalRule.length == 0
    ) {
      searchStore.formalRuleLicenceContract = false;
      searchStore.formalRuleOpenContentLicence = false;
      searchStore.formalRuleUserAgreement = false;
      return;
    }
    bookmark.filterFormalRule.forEach((v: string, index: number): void => {
      if (v == "LICENCE_CONTRACT") {
        searchStore.formalRuleLicenceContract = true;
      } else if (v == "OPEN_CONTENT_LICENCE") {
        searchStore.formalRuleOpenContentLicence = true;
      } else {
        searchStore.formalRuleUserAgreement = true;
      }
    });
  },

  buildFormalRuleFilter(searchStore: any): string | undefined {
    const formalRule: Array<string> = [];
    if (searchStore.formalRuleLicenceContract) {
      formalRule.push("LICENCE_CONTRACT");
    }
    if (searchStore.formalRuleOpenContentLicence) {
      formalRule.push("OPEN_CONTENT_LICENCE");
    }
    if (searchStore.formalRuleUserAgreement) {
      formalRule.push("ZBW_USER_AGREEMENT");
    }
    if (formalRule.length == 0) {
      return undefined;
    } else {
      return formalRule.join(",");
    }
  },

  setTempValFilter(searchStore: any, bookmark: BookmarkRest): void {
    if (
      bookmark.filterTemporalValidity == undefined ||
      bookmark.filterTemporalValidity.length == 0
    ) {
      searchStore.temporalValidityFilterFuture = false;
      searchStore.temporalValidityFilterPresent = false;
      searchStore.temporalValidityFilterPast = false;
      return;
    }
    bookmark.filterTemporalValidity.forEach((v: string): void => {
      if (v == "FUTURE") {
        searchStore.temporalValidityFilterFuture = v;
      } else if (v == "PAST") {
        searchStore.temporalValidityFilterPast = v;
      } else {
        searchStore.temporalValidityFilterPresent = v;
      }
    });
  },

  buildTempValFilter(searchStore: any): string | undefined {
    const tempVal: Array<string> = [];
    if (searchStore.temporalValidityFilterFuture) {
      tempVal.push("FUTURE");
    }
    if (searchStore.temporalValidityFilterPast) {
      tempVal.push("PAST");
    }
    if (searchStore.temporalValidityFilterPresent) {
      tempVal.push("PRESENT");
    }
    if (tempVal.length == 0) {
      return undefined;
    } else {
      return tempVal.join(",");
    }
  },

  setStartDateAtFilter(searchStore: any, bookmark: BookmarkRest): void {
    if (bookmark.filterStartDate == undefined) {
      searchStore.temporalEventState.startDateOrEndDateValue = undefined;
      return;
    }
    searchStore.temporalEventState.startDateOrEndDateOption = "startDate";
    searchStore.temporalEventState.startDateOrEndDateValue =
      bookmark.filterStartDate;
  },

  buildStartDateAtFilter(searchStore: any): string | undefined {
    if (
      searchStore.temporalEventState.startDateOrEndDateOption == "startDate" &&
      searchStore.temporalEventState.startDateOrEndDateFormattedValue != undefined &&
      searchStore.temporalEventState.startDateOrEndDateFormattedValue != ""
    ) {
      return searchStore.temporalEventState.startDateOrEndDateFormattedValue;
    } else {
      return undefined;
    }
  },

  setEndDateAtFilter(searchStore: any, bookmark: BookmarkRest): void {
    if (bookmark.filterEndDate == undefined) {
      searchStore.temporalEventState.startDateOrEndDateValue = undefined;
      return;
    }
    searchStore.temporalEventState.startDateOrEndDateOption = "endDate";
  },

  buildEndDateAtFilter(searchStore: any): string | undefined {
    if (
      searchStore.temporalEventState.startDateOrEndDateOption == "endDate" &&
      searchStore.temporalEventState.startDateOrEndDateFormattedValue != undefined &&
      searchStore.temporalEventState.startDateOrEndDateFormattedValue != ""
    ) {
      return searchStore.temporalEventState.startDateOrEndDateFormattedValue;
    } else {
      return undefined;
    }
  },

  setPublicationTypeFilter(searchStore: any, bookmark: BookmarkRest): void {
    if (
      bookmark.filterPublicationType == undefined ||
      bookmark.filterPublicationType.length == 0
    ) {
      searchStore.publicationTypeIdx = [];
      return;
    }
    searchStore.publicationTypeIdx = Array(
      bookmark.filterPublicationType.length,
    ).fill(true);
    searchStore.publicationTypeReceived = Array(
      bookmark.filterPublicationType.length,
    );
    bookmark.filterPublicationType.forEach((v: string, index: number): void => {
      searchStore.publicationTypeReceived[index] = {
        count: 0,
        publicationType: v.toLowerCase(),
      } as PublicationTypeWithCountRest;
    });
  },

  buildPublicationTypeFilter(searchStore: any): string | undefined {
    const types: Array<string> = [];
    const typesFrontend: Array<string> = [];
    searchStore.publicationTypeIdx.forEach(
      (i: boolean | undefined, index: number): void => {
        if (i) {
          let modifiedPubTypeFilter: string;
          switch (
            searchStore.publicationTypeReceived[
              index
            ].publicationType.toString()
          ) {
            case "article":
              modifiedPubTypeFilter = "ARTICLE";
              break;
            case "book":
              modifiedPubTypeFilter = "BOOK";
              break;
            case "book_part":
              modifiedPubTypeFilter = "BOOK_PART";
              break;
            case "conference_paper":
              modifiedPubTypeFilter = "CONFERENCE_PAPER";
              break;
            case "periodical_part":
              modifiedPubTypeFilter = "PERIODICAL_PART";
              break;
            case "proceeding":
              modifiedPubTypeFilter = "PROCEEDING";
              break;
            case "research_report":
              modifiedPubTypeFilter = "RESEARCH_REPORT";
              break;
            case "thesis":
              modifiedPubTypeFilter = "THESIS";
              break;
            case "working_paper":
              modifiedPubTypeFilter = "WORKING_PAPER";
              break;
            case "other":
              modifiedPubTypeFilter = "OTHER";
              break;
            default:
              modifiedPubTypeFilter = "ERROR";
          }
          types.push(modifiedPubTypeFilter);
          typesFrontend.push(
            searchStore.publicationTypeReceived[index].publicationType,
          );
        }
      },
    );
    // Remind selected ids, for resetting the filter afterwards correctly.
    searchStore.publicationTypeSelectedLastSearch = typesFrontend;
    if (types.length == 0) {
      return undefined;
    } else {
      return types.join(",");
    }
  },

  setValidOnFilter(searchStore: any, bookmark: BookmarkRest): void {
    if (bookmark.filterValidOn == undefined) {
      searchStore.temporalValidOn = undefined;
      return;
    }
    searchStore.temporalValidOn = bookmark.filterValidOn;
  },

  buildValidOnFilter(searchStore: any): string | undefined {
    if (searchStore.temporalValidOnFormatted != undefined && searchStore.temporalValidOnFormatted != "") {
      return searchStore.temporalValidOnFormatted;
    } else {
      return undefined;
    }
  },

  setNoRightInformationFilter(searchStore: any, bookmark: BookmarkRest): void {
    if (bookmark.filterNoRightInformation == undefined) {
      searchStore.noRightInformation = false;
      return;
    }
    searchStore.noRightInformation = bookmark.filterNoRightInformation;
  },

  buildNoRightInformation(searchStore: any): string | undefined {
    if (searchStore.noRightInformation) {
      return "true";
    } else {
      return undefined;
    }
  },

  accessStateToType(a: string): AccessStateRest {
    switch (a) {
      case "open":
        return AccessStateRest.Open;
      case "closed":
        return AccessStateRest.Closed;
      default:
        return AccessStateRest.Restricted;
    }
  },

  publicationTypeToType(t: string): PublicationTypeRest {
    console.log(t);
    switch (t) {
      case "article":
        return PublicationTypeRest.Article;
      case "book":
        return PublicationTypeRest.Book;
      case "book_part":
        return PublicationTypeRest.BookPart;
      case "conference_paper":
        return PublicationTypeRest.ConferencePaper;
      case "periodical_part":
        return PublicationTypeRest.PeriodicalPart;
      case "research_report":
        return PublicationTypeRest.ResearchReport;
      case "thesis":
        return PublicationTypeRest.Thesis;
      case "other":
        return PublicationTypeRest.Other;
      default:
        return PublicationTypeRest.WorkingPaper;
    }
  },
};
