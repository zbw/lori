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

  const hasLicenceContract = ref(false);
  const hasOpenContentLicence = ref(false);
  const manualRight = ref(false);
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
    startDateOrEndDateFormattedValue: "",
    startDateOrEndDateOption: "",
  });

  const temporalValidityFilterFuture = ref(false);
  const temporalValidityFilterPresent = ref(false);
  const temporalValidityFilterPast = ref(false);
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
    formalRuleLicenceContract,
    formalRuleOpenContentLicence,
    formalRuleUserAgreement,
    handleURLResolver,
    hasLicenceContract,
    hasOpenContentLicence,
    hasZbwUserAgreement,
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
    publicationDateFrom,
    publicationDateTo,
    searchTerm,
    seriesIdx,
    seriesReceived,
    seriesSelectedLastSearch,
    stage,
    templateNameIdx,
    templateNameReceived,
    templateNameSelectedLastSearch,
    temporalEventState,
    temporalValidityFilterFuture,
    temporalValidityFilterPast,
    temporalValidityFilterPresent,
    temporalValidOnFormatted,
    zdbIdIdx,
    zdbIdReceived,
    zdbIdSelectedLastSearch,
  };
});
