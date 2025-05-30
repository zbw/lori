import { defineStore } from "pinia";
import { reactive, Ref, ref } from "vue";
import {
  AccessStateWithCountRest, IsPartOfSeriesCountRest, LicenceUrlCountRest,
  PaketSigelWithCountRest,
  PublicationTypeWithCountRest,
  ZdbIdWithCountRest,
} from "@/generated-sources/openapi";
import { TemplateNameWithCountRest } from "@/generated-sources/openapi/models/TemplateNameWithCountRest";

export const useSearchStore = defineStore("search", () => {
  const searchTerm = ref("");
  const lastSearchTerm = ref("");
  const isLastSearchForTemplates = ref(false);

  const accessStateIdx: Ref<Array<boolean>> = ref([]);
  const accessStateReceived: Ref<Array<AccessStateWithCountRest>> = ref([]);
  const accessStateSelectedLastSearch: Ref<Array<string>> = ref([]);

  const accessStateClosed = ref(false);
  const accessStateOpen = ref(false);
  const accessStateRestricted = ref(false);

  const licenceContracts = ref(0);
  const ccLicenceNoRestrictions = ref(0);
  const noLegalRisks = ref(0);
  const zbwUserAgreements = ref(0);

  const manualRight = ref(false);
  const noRightInformation = ref(false);

  const formalRuleLicenceContract = ref(false);
  const formalRuleCCNoRestriction = ref(false);
  const formalRuleNoLegalRisk = ref(false);
  const formalRuleUserAgreement = ref(false);

  const publicationYearFrom = ref("");
  const publicationYearTo = ref("");

  const paketSigelIdIdx: Ref<Array<boolean>> = ref([]);
  const paketSigelIdReceived: Ref<Array<PaketSigelWithCountRest>> = ref([]);
  const paketSigelSelectedLastSearch: Ref<Array<string>> = ref([]);

  const publicationTypeIdx: Ref<Array<boolean>> = ref([]);
  const publicationTypeReceived: Ref<Array<PublicationTypeWithCountRest>> = ref(
    [],
  );
  const publicationTypeSelectedLastSearch: Ref<Array<string>> = ref([]);

  const temporalEventState = reactive({
    startDateOrEndDateFormattedValue: "",
    startDateOrEndDateOption: "",
  });

  const temporalValidOnFormatted = ref("");

  const zdbIdIdx: Ref<Array<boolean>> = ref([]);
  const zdbIdReceived: Ref<Array<ZdbIdWithCountRest>> = ref([]);
  const zdbIdSelectedLastSearch: Ref<Array<string>> = ref([]);

  const templateNameIdx: Ref<Array<boolean>> = ref([]);
  const templateNameReceived: Ref<Array<TemplateNameWithCountRest>> = ref([]);
  const templateNameSelectedLastSearch: Ref<Array<string>> = ref([]);

  const seriesIdx: Ref<Array<boolean>> = ref([]);
  const seriesReceived: Ref<Array<IsPartOfSeriesCountRest>> = ref([]);
  const seriesSelectedLastSearch: Ref<Array<string>> = ref([]);

  const licenceUrlIdx: Ref<Array<boolean>> = ref([]);
  const licenceUrlReceived: Ref<Array<LicenceUrlCountRest>> = ref([]);
  const licenceUrlSelectedLastSearch: Ref<Array<string>> = ref([]);

  const accessStateOnDateState = reactive({
    dateValueFormatted: "",
    accessState: "",
  });
  const accessStateOnDateReceived: Ref<Array<AccessStateWithCountRest>> = ref([]);
  const accessStateOnDateIdx: Ref<Array<string>> = ref([]);

  // Deployment Stage
  const stage = ref("");
  const handleURLResolver = ref("");

  return {
    accessStateOnDateIdx,
    accessStateOnDateReceived,
    accessStateOnDateState,
    lastSearchTerm,
    accessStateIdx,
    accessStateSelectedLastSearch,
    accessStateClosed,
    accessStateRestricted,
    accessStateOpen,
    accessStateReceived,
    formalRuleNoLegalRisk,
    formalRuleCCNoRestriction,
    formalRuleLicenceContract,
    formalRuleUserAgreement,
    handleURLResolver,
    ccLicenceNoRestrictions,
    licenceContracts,
    noLegalRisks,
    zbwUserAgreements,
    manualRight,
    noRightInformation,
    isLastSearchForTemplates,
    licenceUrlIdx,
    licenceUrlReceived,
    licenceUrlSelectedLastSearch,
    paketSigelIdIdx,
    paketSigelIdReceived,
    paketSigelSelectedLastSearch,
    publicationTypeIdx,
    publicationTypeReceived,
    publicationTypeSelectedLastSearch,
    publicationYearFrom,
    publicationYearTo,
    searchTerm,
    seriesIdx,
    seriesReceived,
    seriesSelectedLastSearch,
    stage,
    templateNameIdx,
    templateNameReceived,
    templateNameSelectedLastSearch,
    temporalEventState,
    temporalValidOnFormatted,
    zdbIdIdx,
    zdbIdReceived,
    zdbIdSelectedLastSearch,
  };
});
