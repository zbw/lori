const HANDLE_URL = "https://hdl.handle.net/";
export default {
  shortenHandle(handle: string): string {
    if (handle.startsWith("http")) {
      return handle.substring(22);
    } else {
      return handle;
    }
  },

  hrefHandle(handle: string): string {
    if (handle.startsWith("https")) {
      return handle;
    }
    if (handle.startsWith("http")) {
      return "https" + handle.substring(4);
    }
    return HANDLE_URL + handle;
  },

  prependHandleUrl(handlePath: string | undefined): string {
    if (handlePath == undefined) {
      return HANDLE_URL;
    }
    return HANDLE_URL + handlePath;
  },
  prettyPrintPublicationType(pubType: string): string {
    switch (pubType) {
      case "article":
        return "Aufsatz/Article";
      case "book":
        return "Buch/Book";
      case "bookPart":
        return "Buchaufsatz/Book Part";
      /**
       * IMPORTANT NOTE: Openapis conversion of enums between frontend and backend
       * has issues with multiple word entries. The entries aren't always
       * encoded as the interface suggests, for instance 'periodicalPart' is
       * sometimes encoded as 'periodical_part'. That's the reason why all
       * these conversions contain both variants.
       */
      case "conference_paper":
        return "Konferenzschrift/\n Conference Paper ";
      case "conferencePaper":
        return "Konferenzschrift/\n Conference Paper ";
      case "periodical_part":
        return "Zeitschriftenband/\n Periodical Part ";
      case "periodicalPart":
        return "Zeitschriftenband/\n Periodical Part ";
      case "proceedings":
        return "Konferenzband/\n Proceeding ";
      case "research_report":
        return "Forschungsbericht/\n Research Report ";
      case "researchReport":
        return "Forschungsbericht/\n Research Report ";
      case "thesis":
        return "Thesis ";
      case "working_paper":
        return "Working Paper ";
      case "workingPaper":
        return "Working Paper ";
      default:
        return "Unbekannter Pubikationstyp: " + pubType;
    }
  },
};
