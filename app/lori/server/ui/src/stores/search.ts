import { defineStore } from "pinia";
import { reactive, Ref, ref } from "vue";
import {
  AccessStateWithCountRest,
  PaketSigelWithCountRest,
  PublicationTypeWithCountRest,
  ZdbIdWithCountRest,
} from "@/generated-sources/openapi";

export const useSearchStore = defineStore("search", () => {
  const lastSearchTerm = ref("");

  const accessStateIdx: Ref<Array<boolean>> = ref([]);
  const accessStateReceived: Ref<Array<AccessStateWithCountRest>> = ref([]);
  const accessStateSelectedLastSearch: Ref<Array<string>> = ref([]);
  const accessStateClosed = ref(false);
  const accessStateOpen = ref(false);
  const accessStateRestricted = ref(false);

  const hasLicenceContract = ref(false);
  const hasOpenContentLicence = ref(false);
  const noRightInformation = ref(false);
  const hasZbwUserAgreement = ref(false);

  const formalRuleLicenceContract = ref(false);
  const formalRuleOpenContentLicence = ref(false);
  const formalRuleUserAgreement = ref(false);

  const publicationDateFrom = ref("");
  const publicationDateTo = ref("");

  const paketSigelIdIdx: Ref<Array<boolean>> = ref([]);
  const paketSigelIdReceived: Ref<Array<PaketSigelWithCountRest>> = ref([]);
  const paketSigelSelectedLastSearch: Ref<Array<string>> = ref([]);

  const publicationTypeIdx: Ref<Array<boolean>> = ref([]);
  const publicationTypeReceived: Ref<Array<PublicationTypeWithCountRest>> = ref(
    [],
  );
  const publicationTypeSelectedLastSearch: Ref<Array<string>> = ref([]);

  const temporalEventState = reactive({
    startDateOrEndDateValue: "",
    startDateOrEndDateOption: "",
  });

  const temporalValidityFilterFuture = ref(false);
  const temporalValidityFilterPresent = ref(false);
  const temporalValidityFilterPast = ref(false);
  const temporalValidOn = ref(undefined as Date | undefined);

  const zdbIdIdx: Ref<Array<boolean>> = ref([]);
  const zdbIdReceived: Ref<Array<ZdbIdWithCountRest>> = ref([]);
  const zdbIdSelectedLastSearch: Ref<Array<string>> = ref([]);

  return {
    lastSearchTerm,
    accessStateIdx,
    accessStateSelectedLastSearch,
    accessStateClosed,
    accessStateRestricted,
    accessStateOpen,
    accessStateReceived,
    formalRuleLicenceContract,
    formalRuleOpenContentLicence,
    formalRuleUserAgreement,
    hasLicenceContract,
    hasOpenContentLicence,
    hasZbwUserAgreement,
    noRightInformation,
    paketSigelIdIdx,
    paketSigelIdReceived,
    paketSigelSelectedLastSearch,
    publicationTypeIdx,
    publicationTypeReceived,
    publicationTypeSelectedLastSearch,
    publicationDateFrom,
    publicationDateTo,
    temporalEventState,
    temporalValidityFilterFuture,
    temporalValidityFilterPast,
    temporalValidityFilterPresent,
    temporalValidOn,
    zdbIdIdx,
    zdbIdReceived,
    zdbIdSelectedLastSearch,
  };
});
