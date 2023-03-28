export default {
  buildPublicationDateFilter(searchStore: any): string | undefined {
    return searchStore.publicationDateFrom == "" &&
      searchStore.publicationDateTo == ""
      ? undefined
      : searchStore.publicationDateFrom + "-" + searchStore.publicationDateTo;
  },

  buildPaketSigelIdFilter(searchStore: any): string | undefined {
    const paketSigelIds: Array<string> = [];
    searchStore.paketSigelIdIdx.forEach(
      (i: boolean | undefined, index: number): void => {
        if (i) {
          paketSigelIds.push(
            searchStore.paketSigelIdReceived[index].paketSigel
          );
        }
      }
    );
    // Remind selected ids, for resetting the filter afterwards correctly.
    searchStore.paketSigelSelectedLastSearch = paketSigelIds;
    if (paketSigelIds.length == 0) {
      return undefined;
    } else {
      return paketSigelIds.join(",");
    }
  },

  buildZDBIdFilter(searchStore: any): string | undefined {
    const zdbIds: Array<string> = [];
    searchStore.zdbIdIdx.forEach(
      (i: boolean | undefined, index: number): void => {
        if (i) {
          zdbIds.push(searchStore.zdbIdReceived[index].zdbId);
        }
      }
    );
    // Remind selected ids, for resetting the filter afterwards correctly.
    searchStore.zdbIdSelectedLastSearch = zdbIds;
    if (zdbIds.length == 0) {
      return undefined;
    } else {
      return zdbIds.join(",");
    }
  },

  buildAccessStateFilter(searchStore: any): string | undefined {
    const accessStates: Array<string> = [];
    searchStore.accessStateIdx.forEach(
      (i: boolean | undefined, index: number): void => {
        if (i) {
          accessStates.push(
            searchStore.accessStateReceived[index].accessState.toUpperCase()
          );
        }
      }
    );

    // Remind selected ids, for resetting the filter afterwards correctly.
    searchStore.accessStateSelectedLastSearch = accessStates.map((value) =>
      value.toLowerCase()
    );
    if (accessStates.length == 0) {
      return undefined;
    } else {
      return accessStates.join(",");
    }
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
  buildStartDateAtFilter(searchStore: any): string | undefined {
    if (
      searchStore.temporalEventStartDateFilter &&
      searchStore.temporalEventInput != ""
    ) {
      return searchStore.temporalEventInput;
    } else {
      return undefined;
    }
  },

  buildEndDateAtFilter(searchStore: any): string | undefined {
    if (
      searchStore.temporalEventEndDateFilter &&
      searchStore.temporalEventInput != ""
    ) {
      return searchStore.temporalEventInput;
    } else {
      return undefined;
    }
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
            case "bookPart":
              modifiedPubTypeFilter = "BOOK_PART";
              break;
            case "book_part":
              modifiedPubTypeFilter = "BOOK_PART";
              break;
            case "conferencePaper":
              modifiedPubTypeFilter = "CONFERENCE_PAPER";
              break;
            case "periodicalPart":
              modifiedPubTypeFilter = "PERIODICAL_PART";
              break;
            case "proceedings":
              modifiedPubTypeFilter = "PROCEEDING";
              break;
            case "researchReport":
              modifiedPubTypeFilter = "RESEARCH_REPORT";
              break;
            case "thesis":
              modifiedPubTypeFilter = "THESIS";
              break;
            case "workingPaper":
              modifiedPubTypeFilter = "WORKING_PAPER";
              break;
            default:
              modifiedPubTypeFilter = "ERROR";
          }
          types.push(modifiedPubTypeFilter);
          typesFrontend.push(
            searchStore.publicationTypeReceived[index].publicationType
          );
        }
      }
    );
    // Remind selected ids, for resetting the filter afterwards correctly.
    searchStore.publicationTypeSelectedLastSearch = typesFrontend;
    if (types.length == 0) {
      return undefined;
    } else {
      return types.join(",");
    }
  },

  buildValidOnFilter(searchStore: any): string | undefined {
    if (
      searchStore.temporalValidOn != undefined &&
      searchStore.temporalValidOn != ""
    ) {
      return searchStore.temporalValidOn;
    } else {
      return undefined;
    }
  },

  buildNoRightInformation(searchStore: any): string | undefined {
    if (searchStore.noRightInformation) {
      return "true";
    } else {
      return undefined;
    }
  },
};
