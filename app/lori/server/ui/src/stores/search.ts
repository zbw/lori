import { defineStore } from "pinia";
import { computed, ref } from "vue";

export const useSearchStore = defineStore("search", () => {
  const lastSearchTerm = ref("");

  const publicationDateFrom = ref("");
  const publicationDateTo = ref("");

  const publicationTypeArticle = ref(false);
  const publicationTypeBook = ref(false);
  const publicationTypeBookPart = ref(false);
  const publicationTypeConferencePaper = ref(false);
  const publicationTypePeriodicalPart = ref(false);
  const publicationTypeProceedings = ref(false);
  const publicationTypeResearchReport = ref(false);
  const publicationTypeThesis = ref(false);
  const publicationTypeWorkingPaper = ref(false);

  return {
    lastSearchTerm,
    publicationDateFrom,
    publicationDateTo,
    publicationTypeArticle,
    publicationTypeBook,
    publicationTypeBookPart,
    publicationTypeConferencePaper,
    publicationTypePeriodicalPart,
    publicationTypeProceedings,
    publicationTypeResearchReport,
    publicationTypeThesis,
    publicationTypeWorkingPaper,
  };
});
