import { defineStore } from "pinia";
import { computed, ref } from "vue";

export const useSearchStore = defineStore("search", () => {
  const lastSearchTerm = ref("");

  const accessStateClosed = ref(false);
  const accessStateOpen = ref(false);
  const accessStateRestricted = ref(false);

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

  const temporalEventInput = ref("");
  const temporalEventStartDateFilter = ref(false);
  const temporalEventEndDateFilter = ref(false);

  const temporalValidityFilterFuture = ref(false);
  const temporalValidityFilterPresent = ref(false);
  const temporalValidityFilterPast = ref(false);

  return {
    lastSearchTerm,
    accessStateClosed,
    accessStateRestricted,
    accessStateOpen,
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
    temporalEventInput,
    temporalEventStartDateFilter,
    temporalEventEndDateFilter,
    temporalValidityFilterFuture,
    temporalValidityFilterPast,
    temporalValidityFilterPresent,
  };
});
