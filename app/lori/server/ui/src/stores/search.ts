import { defineStore } from "pinia";
import { Ref, ref } from "vue";

export const useSearchStore = defineStore("search", () => {
  const lastSearchTerm = ref("");

  const accessStateClosed = ref(false);
  const accessStateOpen = ref(false);
  const accessStateRestricted = ref(false);

  const availableAccessState: Ref<Array<string>> = ref([]);
  const availablePaketSigelIds: Ref<Array<string>> = ref([]);
  const availablePublicationTypes: Ref<Array<string>> = ref([]);
  const availableZDBIds: Ref<Array<string>> = ref([]);

  const hasLicenceContract = ref(false);
  const hasOpenContentLicence = ref(false);
  const hasZbwUserAgreement = ref(false);

  const formalRuleLicenceContract = ref(false);
  const formalRuleOpenContentLicence = ref(false);
  const formalRuleUserAgreement = ref(false);

  const publicationDateFrom = ref("");
  const publicationDateTo = ref("");

  const accessStateIdx: Ref<Array<boolean>> = ref([]);
  const paketSigelIdIdx: Ref<Array<boolean>> = ref([]);
  const publicationTypeIdx: Ref<Array<boolean>> = ref([]);
  const zdbIdIdx: Ref<Array<boolean>> = ref([]);

  const temporalEventInput = ref("");
  const temporalEventStartDateFilter = ref(false);
  const temporalEventEndDateFilter = ref(false);

  const temporalValidityFilterFuture = ref(false);
  const temporalValidityFilterPresent = ref(false);
  const temporalValidityFilterPast = ref(false);

  const temporalValidOn = ref("");

  return {
    lastSearchTerm,
    accessStateIdx,
    accessStateClosed,
    accessStateRestricted,
    accessStateOpen,
    availableAccessState,
    availablePaketSigelIds,
    availablePublicationTypes,
    availableZDBIds,
    formalRuleLicenceContract,
    formalRuleOpenContentLicence,
    formalRuleUserAgreement,
    hasLicenceContract,
    hasOpenContentLicence,
    hasZbwUserAgreement,
    paketSigelIdIdx,
    publicationTypeIdx,
    publicationDateFrom,
    publicationDateTo,
    temporalEventInput,
    temporalEventStartDateFilter,
    temporalEventEndDateFilter,
    temporalValidityFilterFuture,
    temporalValidityFilterPast,
    temporalValidityFilterPresent,
    temporalValidOn,
    zdbIdIdx,
  };
});
