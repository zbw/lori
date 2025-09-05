<script lang="ts">
import {
  AboutRest,
  BookmarkRest, GroupRest,
  ItemInformation,
  ItemRest,
  RightRest,
} from "@/generated-sources/openapi";
import api from "@/api/api";
import GroupEdit from "@/components/GroupEdit.vue";
import GroupOverview from "@/components/GroupOverview.vue";
import MetadataView from "@/components/MetadataView.vue";
import RightsView from "@/components/RightsView.vue";
import SearchFilter from "@/components/SearchFilter.vue";
import {computed, defineComponent, onMounted, Ref, ref, watch} from "vue";
import { useSearchStore } from "@/stores/search";
import { useDialogsStore } from "@/stores/dialogs";
import searchquerybuilder from "@/utils/searchquerybuilder";
import url from "@/utils/url";
import error from "@/utils/error";
import BookmarkSave from "@/components/BookmarkSave.vue";
import TemplateOverview from "@/components/TemplateOverview.vue";
import BookmarkOverview from "@/components/BookmarkOverview.vue";
import templateApi from "@/api/templateApi";
import RightsEditDialog from "@/components/RightsEditDialog.vue";
import metadata_utils from "@/utils/metadata_utils";
import {VResizeDrawer} from "@wdns/vuetify-resize-drawer";
import Dashboard from "@/components/Dashboard.vue";
import {useUserStore} from "@/stores/user";
import ResizableDialog from "@/components/ResizableDialog.vue";
import TopNavigationBar from "@/components/TopNavigationBar.vue";
import bookmarkApi from "@/api/bookmarkApi";
import {DataTableOptions, ReadonlyDataTableHeader} from "@/types/vuetify";
import {RouteLocationNormalizedLoaded, Router, useRoute, useRouter} from "vue-router";

export default defineComponent({
  computed: {
    metadata_utils() {
      return metadata_utils;
    },
  },
  components: {
    GroupEdit,
    TopNavigationBar,
    ResizableDialog,
    Dashboard,
    VResizeDrawer,
    RightsEditDialog,
    BookmarkOverview,
    TemplateOverview,
    BookmarkSave,
    GroupOverview,
    RightsView,
    MetadataView,
    SearchFilter,
  },

  setup() {
    /**
     * Router + Route
     */
    const router: Router = useRouter()
    const route: RouteLocationNormalizedLoaded = useRoute()
    /**
     * Stores:
     */
    const searchStore = useSearchStore();
    const userStore = useUserStore();

    /**
     * Table:
     */
    const items: Ref<Array<ItemRest>> = ref([]);
    const currentItem = ref({} as ItemRest);
    const selectedItems: Ref<Array<string>> = ref([]);
    const tableContentLoading = ref(true);

    const headers: ReadonlyDataTableHeader[] = [
      {
        title: "Titel",
        sortable: true,
        value: "title",
        width: "300px",
      },
      {
        title: "Handle",
        sortable: true,
        value: "handle",
      },
      {
        title: "Community",
        sortable: true,
        value: "communityName",
      },
      {
        title: "Collection",
        sortable: true,
        value: "collectionName",
      },
      {
        title: "Publikationstyp",
        sortable: true,
        value: "publicationType",
      },
      {
        title: "Publikationsjahr",
        sortable: true,
        value: "publicationYear",
      },
      {
        title: "Band",
        sortable: true,
        value: "band",
      },
      {
        title: "DOI",
        sortable: true,
        value: "doi",
      },
      {
        title: "ISBN",
        sortable: true,
        value: "isbn",
      },
      {
        title: "ISSN",
        sortable: true,
        value: "issn",
      },
      {
        title: "Paket-Sigel",
        sortable: true,
        value: "paketSigel",
      },
      {
        title: "PPN",
        sortable: true,
        value: "ppn",
      },
      {
        title: "Titel Journal",
        sortable: true,
        value: "titleJournal",
      },
      {
        title: "Titel Serie",
        sortable: true,
        value: "titleSeries",
      },
      {
        title: "ZDB-ID (Journal + Serie)",
        sortable: true,
        value: "zdbIds",
      },
      {
        title: "Serie",
        sortable: true,
        value: "isPartOfSeries",
      },
    ];

    const selectedHeaders = ref(headers.slice(0, 6));
    const headersValueVSelect = ref(selectedHeaders.value);

    const currentPage = ref(1);
    const currentRightId = ref("");
    const pageSize = ref("10"); // initial page size
    const pageSizes = ref<Array<string>>(["5", "10", "25", "50", "Alle"]);
    const totalPages = ref(0);
    const numberOfResults = ref(0);

    const pageSizeComputed = computed(() => {
      if (pageSize.value == "Alle"){
        return -1;
      } else {
        return parseInt(pageSize.value);
      }
    });

    // Page changes
    const handlePageChange = () => {
      if (templateSearchIsActive.value) {
        executeSearchByRightId(currentRightId.value);
      } else {
        searchQuery();
      }
    };

    const handlePageSizeChange = () => {
      currentPage.value = 1;
      if (templateSearchIsActive.value) {
        executeSearchByRightId(currentRightId.value);
      } else {
        searchQuery();
      }
    };

    /**
     * Row selection management.
     *
     * Single Click
     */
    const addActiveItem = (mouseEvent: MouseEvent, row: any) => {
      const item: ItemRest | undefined = items.value.find(
        (e) => e.metadata.handle === row.item.handle,
      );
      if (item !== undefined) {
        currentItem.value = item;
        selectedItems.value = [row.item.handle];
      }
    };

    /** Double Click **/
    const setActiveItem = (mouseEvent: MouseEvent, row: any) => {
      //row.select(true);
      const item: ItemRest | undefined = items.value.find(
        (e) => e.metadata.handle === row.item.handle,
      );
      if (item !== undefined) {
        currentItem.value = item;
      }
      selectedItems.value = selectedItems.value.filter(
        (e: string) => e == row.item.handle,
      );
    };

    onMounted(() => {
      loadTemplateView();
      loadGroupView();
      loadBookmarkView();
      const hasMetadataParameter = loadMetadataView();
      const hasInitSearch = loadInitSearchQuery();
      const hasInitBookmarkId = loadInitBookmarkId();
      if (!hasMetadataParameter && !hasInitSearch && !hasInitBookmarkId) {
        startSearch();
      }
      // Always load about information from backend
      loadBackendParameters();
    });

    watch(headersValueVSelect, (currentValue) => {
      selectedHeaders.value = currentValue;
    });

    watch(currentPage, () => {
      handlePageChange();
    });

    watch(pageSize, () => {
      handlePageSizeChange();
    });

    // Initial Group View
    const queryParameterBookmark = ref({} as BookmarkRest);
    const bookmarkSaveQueryParameterDialog = ref(false);

    const loadBookmarkView: () => boolean = () => {
      const urlParams = new URLSearchParams(window.location.search);
      const bookmarkId: string | null = urlParams.get(url.QUERY_PARAMETER_BOOKMARK_ID);
      if (bookmarkId == null || bookmarkId == "") {
        return false;
      }
      bookmarkApi
          .getBookmarkById(parseInt(bookmarkId))
          .then((response: BookmarkRest) => {
            queryParameterBookmark.value = response;
            bookmarkSaveQueryParameterDialog.value = true;
          })
          .catch((e) => {
            error.errorHandling(e, (errMsg: string) => {
              errorMsg.value = errMsg;
              errorMsgIsActive.value = true;
            });
          });
      return true;
    };

    // Initial Group View
    const queryParameterGroup = ref({} as GroupRest);
    const groupEditActivated = ref(false);
    const loadGroupView: () => boolean = () => {
      const urlParams = new URLSearchParams(window.location.search);
      const groupId: string | null = urlParams.get(url.QUERY_PARAMETER_GROUP_ID);
      if (groupId == null || groupId == "") {
        return false;
      }
      api
          .getGroupById(
              parseInt(groupId),
              undefined,
          )
          .then((response: GroupRest) => {
            queryParameterGroup.value = response;
            groupEditActivated.value = true;
          })
          .catch((e) => {
            error.errorHandling(e, (errMsg: string) => {
              errorMsg.value = errMsg;
              errorMsgIsActive.value = true;
            });
          });
      return true;
    };

    const closeGroupEditDialog = () => {
      groupEditActivated.value = false;
    };

    // Initial Template View
    const templateLoadError = ref(false);
    const templateLoadErrorMsg = ref("");
    const queryParameterRight = ref({} as RightRest);
    const rightEditActivated = ref(false);
    const loadTemplateView: () => boolean = () => {
      const urlParams = new URLSearchParams(window.location.search);
      const templateId: string | null = urlParams.get(url.QUERY_PARAMETER_TEMPLATE_ID);
      if (templateId == null || templateId == "") {
        return false;
      }
      templateApi
        .getTemplateById(templateId)
        .then((response: RightRest) => {
          queryParameterRight.value = response;
          rightEditActivated.value = true;
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            templateLoadErrorMsg.value = errMsg;
            templateLoadError.value = true;
          });
        });
      return true;
    };

    const getRightPP: () => string | null = () => {
      const urlParams = new URLSearchParams(window.location.search);
      const rightId: string | null = urlParams.get(url.QUERY_PARAMETER_RIGHT_ID);
      if (rightId == null || rightId == "") {
        return null;
      } else {
        return rightId;
      }
    };

    const loadInitBookmarkId: () => boolean = () => {
      const urlParams = new URLSearchParams(window.location.search);
      const bookmarkId: string | null = urlParams.get(url.QUERY_PARAMETER_EXECUTE_BOOKMARK_ID);
      if (bookmarkId == null || bookmarkId == "") {
        return false;
      }

      const bookmarkIdParsed = parseInt(bookmarkId);
      bookmarkApi.getBookmarkById(bookmarkIdParsed)
          .then((bookmark: BookmarkRest) => {
            executeBookmarkSearch(bookmark)
          }).catch((e) => {
        error.errorHandling(e, (errMsg: string) => {
          tableContentLoading.value = false;
          errorMsg.value = "Laden der bibliographischen Daten war nicht erfolgreich: " + errMsg;
          errorMsgIsActive.value = true;
        });
      });
      return true;
    };

    const loadMetadataView: () => boolean = () => {
      const urlParams = new URLSearchParams(window.location.search);
      const handle: string | null = urlParams.get(url.QUERY_PARAMETER_HANDLE);
      if (handle == null || handle == "") {
        return false;
      }
      searchQueryByTerm("hdl:" + handle, () => {});
      return true;
    };

    const loadInitSearchQuery: () => boolean = () => {
      const urlParams = new URLSearchParams(window.location.search);
      const searchQuery: string | null = urlParams.get(url.QUERY_PARAMETER_DASHBOARD_HANDLE_SEARCH);
      if (searchQuery == null || searchQuery == "") {
        return false;
      }
      startDashboardSearch(searchQuery)
      return true;
    };

    const loadBackendParameters = () => {
      api
        .getAboutInformation()
        .then((response: AboutRest) => {
          searchStore.stage = response.stage;
          searchStore.handleURLResolver = response.handleURL;
          userStore.signInURL = response.duoSSO;
          userStore.signOutURL = response.duoSLO;
          userStore.commitHash = response.commitHash;
          if(response.stage == "dev"){
            document.title = "lori-dev";
          }
          if(response.stage == "qs"){
            document.title = "lori-qs";
          }
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            tableContentLoading.value = false;
            errorMsg.value = "Laden der bibliographischen Daten war nicht erfolgreich: " + errMsg;
            errorMsgIsActive.value = true;
          });
        });
    };

    const startDashboardSearch = (searchTerm: string) => {
      dialogStore.dashboardViewActivated = false;
      searchStore.searchTerm = searchTerm;
      searchQueryByTerm(searchTerm, () => {
        if (items.value.length > 0){
          currentItem.value = items.value[0]
          selectedItems.value = [items.value[0].metadata.handle];
          const rightIdToLoad = getRightPP();
          if (rightIdToLoad == null){
            return;
          } else {
            dialogStore.rightsEditTabsSelectedRight = rightIdToLoad;
            dialogStore.rightsEditTabsActivated = true;
          }
        }
      });
    };

    const closeTemplateEditDialog = () => {
      rightEditActivated.value = false;
    };

    // Messages
    const successMsgIsActive = ref(false);
    const successMsg = ref("");
    const errorMsgIsActive = ref(false);
    const errorMsg = ref("");

    // Search
    const templateSearchIsActive = ref(false);
    const initSearchByRightId = (rightId: string, templateName: string) => {
      searchStore.searchTerm = "";
      templateSearchIsActive.value = true;
      currentPage.value = 1;
      currentRightId.value = rightId;
      closeTemplateOverview();
      successMsg.value =
        "Alle gespeicherten Suchen für Template " +
        "'" + templateName + " (" + rightId + ")'" +
        " wurden ausgeführt.";
      successMsgIsActive.value = true;
      searchStore.isLastSearchForTemplates = true;
      executeSearchByRightId(rightId);
    };

    const getAccessStatesForDate = () => {
      api
          .searchQuery(
              "",
              0,
              1,
              0,
              true,
              false,
              searchquerybuilder.buildPublicationYearFilter(searchStore),
              searchquerybuilder.buildPublicationTypeFilter(searchStore),
              searchquerybuilder.buildAccessStateFilter(searchStore),
              searchquerybuilder.buildStartDateAtFilter(searchStore),
              searchquerybuilder.buildEndDateAtFilter(searchStore),
              searchquerybuilder.buildFormalRuleFilter(searchStore),
              searchquerybuilder.buildValidOnFilter(searchStore),
              searchquerybuilder.buildPaketSigelIdFilter(searchStore),
              searchquerybuilder.buildZDBIdFilter(searchStore),
              searchquerybuilder.buildNoRightInformation(searchStore),
              searchquerybuilder.buildTemplateNameFilter(searchStore),
              searchquerybuilder.buildSeriesFilter(searchStore),
              searchquerybuilder.buildLicenceUrlFilter(searchStore),
              searchquerybuilder.buildManualRightFilter(searchStore),
              searchquerybuilder.buildDeletionsFilter(searchStore),
              searchStore.accessStateOnDateState.dateValueFormatted, // The interesting line
              searchquerybuilder.buildSortBy(datatableOptions.value),
              searchquerybuilder.buildOrderBy(datatableOptions.value),
          ).then((response: ItemInformation) => {
        if (response.accessStateWithCount != undefined) {
          searchStore.accessStateOnDateReceived = response.accessStateWithCount;
        }
      })
          .catch((e) => {
            error.errorHandling(e, (errMsg: string) => {
              tableContentLoading.value = false;
              errorMsg.value = "Fehler beim Ausführen der Suche - " + errMsg;
              errorMsgIsActive.value = true;
            });
          });
    };

    const executeSearchByRightId = (rightId: string) => {
      api
        .searchQuery(
          "",
          (currentPage.value - 1) * pageSizeComputed.value, // offset
          pageSizeComputed.value, // limit
          pageSizeComputed.value,
            false,
          true,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
            rightId,
          undefined,
            undefined,
            undefined,
            undefined,
            undefined,
            searchquerybuilder.buildSortBy(datatableOptions.value),
            searchquerybuilder.buildOrderBy(datatableOptions.value),
        )
        .then((response: ItemInformation) => {
          processSearchResult(response);
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            tableContentLoading.value = false;
            errorMsg.value = "Fehler beim Ausführen der Suche - " + errMsg;
            errorMsgIsActive.value = true;
          });
        });
      api
        .searchQuery(
            "",
            (currentPage.value - 1) * pageSizeComputed.value, // offset
            pageSizeComputed.value, // limit
            pageSizeComputed.value,
            false,
            true,
            undefined,
            undefined,
            undefined,
            undefined,
            undefined,
            undefined,
            undefined,
            undefined,
            undefined,
            undefined,
            rightId,
            undefined,
            undefined,
            undefined,
            undefined,
            undefined,
            searchquerybuilder.buildSortBy(datatableOptions.value),
            searchquerybuilder.buildOrderBy(datatableOptions.value),

        )
        .then((response: ItemInformation) => {
          const worker = new Worker(new URL("@/worker/worker.ts", import.meta.url), { type: 'module' });

          // Send the response to the worker for processing
          worker.postMessage(response);

          // Handle the message from the worker
          worker.onmessage = (event) => {
            const minifiedResponse = event.data;
            getAccessStatesForDate();
            processFacets(minifiedResponse);

            // Optionally, terminate the worker after use
            worker.terminate();
          }
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            tableContentLoading.value = false;
            errorMsg.value = "Fehler beim Ausführen der Suche - " + errMsg;
            errorMsgIsActive.value = true;
          });
        });
    };

    const searchQueryByTerm = (searchTerm: string, callback: () => void) => {
      searchStore.isLastSearchForTemplates = false;
      api
        .searchQuery(
          searchTerm,
          (currentPage.value - 1) * pageSizeComputed.value, // offset
          pageSizeComputed.value, // limit
          currentPage.value,
            false,
          true,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
            undefined,
            undefined,
            undefined,
            searchquerybuilder.buildSortBy(datatableOptions.value),
            searchquerybuilder.buildOrderBy(datatableOptions.value),
        )
        .then((response: ItemInformation) => {
          processSearchResult(response);
          callback();
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            tableContentLoading.value = false;
            errorMsg.value = "Fehler beim Ausführen der Suche - " + errMsg;
            errorMsgIsActive.value = true;
          });
        });
      api
          .searchQuery(
              searchTerm,
              (currentPage.value - 1) * pageSizeComputed.value, // offset
              pageSizeComputed.value, // limit
              currentPage.value,
              true,
              false,
              undefined,
              undefined,
              undefined,
              undefined,
              undefined,
              undefined,
              undefined,
              undefined,
              undefined,
              undefined,
              undefined,
              undefined,
              undefined,
              undefined,
              undefined,
              undefined,
              searchquerybuilder.buildSortBy(datatableOptions.value),
              searchquerybuilder.buildOrderBy(datatableOptions.value),
          )
          .then((response: ItemInformation) => {
            processFacets(response);
          })
          .catch((e) => {
            error.errorHandling(e, (errMsg: string) => {
              tableContentLoading.value = false;
              errorMsg.value = "Fehler beim Ausführen der Suche - " + errMsg;
              errorMsgIsActive.value = true;
            });
          });
    };

    const executeBookmarkSearch = (bookmark: BookmarkRest) => {
      searchquerybuilder.setPublicationYearFilter(searchStore, bookmark);
      searchquerybuilder.setPaketSigelFilter(searchStore, bookmark);
      searchquerybuilder.setPublicationTypeFilter(searchStore, bookmark);
      searchquerybuilder.setZDBFilter(searchStore, bookmark);
      searchquerybuilder.setAccessStateFilter(searchStore, bookmark);
      searchquerybuilder.setFormalRuleFilter(searchStore, bookmark);
      searchquerybuilder.setStartDateAtFilter(searchStore, bookmark);
      searchquerybuilder.setEndDateAtFilter(searchStore, bookmark);
      searchquerybuilder.setValidOnFilter(searchStore, bookmark);
      searchquerybuilder.setNoRightInformationFilter(searchStore, bookmark);
      searchquerybuilder.setTemplateNameFilter(searchStore, bookmark);
      searchquerybuilder.setSeriesFilter(searchStore, bookmark);
      searchquerybuilder.setLicenceUrlFilter(searchStore, bookmark);
      searchquerybuilder.setManualRightFilter(searchStore, bookmark);
      searchquerybuilder.setDeletionsFilter(searchStore, bookmark);
      searchquerybuilder.setAccessStateOnDateFilter(searchStore, bookmark);
      searchStore.searchTerm =
        bookmark.searchTerm != undefined ? bookmark.searchTerm : "";
      closeBookmarkOverview();
      successMsg.value =
        "Die gespeicherte Suche " +
        "'" + bookmark.bookmarkName + " (" + bookmark.bookmarkId + ")'" +
        " wurde erfolgreich ausgeführt.";
      successMsgIsActive.value = true;
      url.addQueryParameters(route, router, {
        [url.QUERY_PARAMETER_EXECUTE_BOOKMARK_ID]: bookmark?.bookmarkId
      });
      startSearch();
    };

    const startEmptySearch = () => {
      searchStore.searchTerm = "";
      startSearch();
    };

    const startSearch = () => {
      currentPage.value = 1;
      pageSize.value = "25";
      if (searchStore.searchTerm == undefined) {
        searchStore.searchTerm = "";
      }
      searchStore.lastSearchTerm = searchStore.searchTerm;
      searchStore.isLastSearchForTemplates = false;
      searchStore.isLastSearchNonTrivialAndSuccessful = false;
      templateSearchIsActive.value = false;
      currentRightId.value = "";
      currentItem.value = {} as ItemRest;
      searchQuery();
    };

    const searchQuery = () => {
      api
        .searchQuery(
          searchStore.searchTerm,
          (currentPage.value - 1) * pageSizeComputed.value,
          pageSizeComputed.value,
          pageSizeComputed.value,
            false,
          true,
          searchquerybuilder.buildPublicationYearFilter(searchStore),
          searchquerybuilder.buildPublicationTypeFilter(searchStore),
          searchquerybuilder.buildAccessStateFilter(searchStore),
          searchquerybuilder.buildStartDateAtFilter(searchStore),
          searchquerybuilder.buildEndDateAtFilter(searchStore),
          searchquerybuilder.buildFormalRuleFilter(searchStore),
          searchquerybuilder.buildValidOnFilter(searchStore),
          searchquerybuilder.buildPaketSigelIdFilter(searchStore),
          searchquerybuilder.buildZDBIdFilter(searchStore),
          searchquerybuilder.buildNoRightInformation(searchStore),
          searchquerybuilder.buildTemplateNameFilter(searchStore),
          searchquerybuilder.buildSeriesFilter(searchStore),
          searchquerybuilder.buildLicenceUrlFilter(searchStore),
          searchquerybuilder.buildManualRightFilter(searchStore),
          searchquerybuilder.buildDeletionsFilter(searchStore),
          searchquerybuilder.buildAccessOnDateFilter(searchStore),
          searchquerybuilder.buildSortBy(datatableOptions.value),
          searchquerybuilder.buildOrderBy(datatableOptions.value),
        )
        .then((response: ItemInformation) => {
          processSearchResult(response);
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            tableContentLoading.value = false;
            errorMsg.value = "Fehler beim Ausführen der Suche - " + errMsg;
            errorMsgIsActive.value = true;
          });
        });
      api
          .searchQuery(
              searchStore.searchTerm,
              (currentPage.value - 1) * pageSizeComputed.value,
              pageSizeComputed.value,
              pageSizeComputed.value,
              true,
              false,
              searchquerybuilder.buildPublicationYearFilter(searchStore),
              searchquerybuilder.buildPublicationTypeFilter(searchStore),
              searchquerybuilder.buildAccessStateFilter(searchStore),
              searchquerybuilder.buildStartDateAtFilter(searchStore),
              searchquerybuilder.buildEndDateAtFilter(searchStore),
              searchquerybuilder.buildFormalRuleFilter(searchStore),
              searchquerybuilder.buildValidOnFilter(searchStore),
              searchquerybuilder.buildPaketSigelIdFilter(searchStore),
              searchquerybuilder.buildZDBIdFilter(searchStore),
              searchquerybuilder.buildNoRightInformation(searchStore),
              searchquerybuilder.buildTemplateNameFilter(searchStore),
              searchquerybuilder.buildSeriesFilter(searchStore),
              searchquerybuilder.buildLicenceUrlFilter(searchStore),
              searchquerybuilder.buildManualRightFilter(searchStore),
              searchquerybuilder.buildDeletionsFilter(searchStore),
              searchquerybuilder.buildAccessOnDateFilter(searchStore),
              searchquerybuilder.buildSortBy(datatableOptions.value),
              searchquerybuilder.buildOrderBy(datatableOptions.value),
          )
          .then((response: ItemInformation) => {
            const worker = new Worker(new URL("@/worker/worker.ts", import.meta.url), { type: 'module' });

            // Send the response to the worker for processing
            worker.postMessage(response);

            // Handle the message from the worker
            worker.onmessage = (event) => {
              const minifiedResponse = event.data;
              getAccessStatesForDate();
              processFacets(minifiedResponse);

              // Optionally, terminate the worker after use
              worker.terminate();
            };
          })
          .catch((e) => {
            error.errorHandling(e, (errMsg: string) => {
              tableContentLoading.value = false;
              errorMsg.value = "Fehler beim Ausführen der Suche - " + errMsg;
              errorMsgIsActive.value = true;
            });
          });
    };

    const processFacets = (response: ItemInformation) => {
      searchStore.licenceContracts = response.licenceContracts;
      searchStore.noLegalRisks = response.noLegalRisks;
      searchStore.ccLicenceNoRestrictions = response.ccLicenceNoRestrictions;
      searchStore.zbwUserAgreements = response.zbwUserAgreements;
      resetAllDynamicFilter(response);
    };

    const processSearchResult = (response: ItemInformation) => {
      searchStore.filtersAsQuery = response.filtersAsQuery != undefined ? response.filtersAsQuery : "";
      items.value = response.itemArray;
      tableContentLoading.value = false;
      totalPages.value = response.totalPages;
      numberOfResults.value = response.numberOfResults;
      if(searchStore.filtersAsQuery != "" || searchStore.searchTerm.trim() != ""){
        searchStore.isLastSearchNonTrivialAndSuccessful = true;
      }
    };

    const resetAllDynamicFilter = (response: ItemInformation) => {
      // Reset AccessState
      searchStore.accessStateReceived =
        response.accessStateWithCount != undefined
          ? [...response.accessStateWithCount]
          : [...Array(0)];
      searchStore.accessStateIdx = Array(
        searchStore.accessStateReceived.length,
      ).fill(false);
      resetDynamicFilter(
        searchStore.accessStateReceived.map((e) => e.accessState),
        searchStore.accessStateSelectedLastSearch,
        searchStore.accessStateIdx,
      );
      // Reset Paket Sigel
      searchStore.paketSigelIdReceived =
        response.paketSigelWithCount != undefined
          ? [...response.paketSigelWithCount]
          : [...Array(0)];
      searchStore.paketSigelIdIdx = Array(
        searchStore.paketSigelIdReceived.length,
      ).fill(false);
      resetDynamicFilter(
        searchStore.paketSigelIdReceived.map((e) => e.paketSigel),
        searchStore.paketSigelSelectedLastSearch,
        searchStore.paketSigelIdIdx,
      );
      // Reset Publication Type
      searchStore.publicationTypeReceived =
        response.publicationTypeWithCount != undefined
          ? [...response.publicationTypeWithCount]
          : [...Array(0)];
      searchStore.publicationTypeIdx = Array(
        searchStore.publicationTypeReceived.length,
      ).fill(false);
      resetDynamicFilter(
        searchStore.publicationTypeReceived.map((e) => e.publicationType),
        searchStore.publicationTypeSelectedLastSearch,
        searchStore.publicationTypeIdx,
      );
      // Reset ZDB Id
      searchStore.zdbIdReceived =
        response.zdbIdWithCount != undefined
          ? [...response.zdbIdWithCount]
          : [...Array(0)];
      searchStore.zdbIdIdx = Array(searchStore.zdbIdReceived.length).fill(
        false,
      );
      resetDynamicFilter(
          searchStore.zdbIdReceived.map((e) => e.zdbId),
          searchStore.zdbIdSelectedLastSearch,
          searchStore.zdbIdIdx,
      );
      // Reset Series
      searchStore.seriesReceived =
          response.isPartOfSeriesCount != undefined
              ? [...response.isPartOfSeriesCount]
              : [...Array(0)];
      searchStore.seriesIdx = Array(searchStore.seriesReceived.length).fill(
          false,
      );
      resetDynamicFilter(
          searchStore.seriesReceived.map((e) => e.series),
          searchStore.seriesSelectedLastSearch,
          searchStore.seriesIdx,
      );
      // Reset Template Names
      searchStore.templateNameReceived =
        response.templateNameWithCount != undefined
          ? [...response.templateNameWithCount]
          : [...Array(0)];
      searchStore.templateNameIdx = Array(
        searchStore.templateNameReceived.length,
      ).fill(false);
      resetDynamicFilter(
        searchStore.templateNameReceived.map((e) => e.rightId),
        searchStore.templateNameSelectedLastSearch,
        searchStore.templateNameIdx,
      );
      // Reset Licence Url
      searchStore.licenceUrlReceived =
          response.licenceUrlCount != undefined
              ? [...response.licenceUrlCount]
              : [...Array(0)];
      searchStore.licenceUrlIdx = Array(
          searchStore.licenceUrlReceived.length,
      ).fill(false);
      resetDynamicFilter(
          searchStore.licenceUrlReceived.map((e) => e.licenceUrl),
          searchStore.licenceUrlSelectedLastSearch,
          searchStore.licenceUrlIdx,
      );
    };

    const resetDynamicFilter = (
      receivedFilters: Array<string>,
      savedFilters: Array<string>,
      idxMap: Array<boolean>,
    ) => {
      receivedFilters.forEach((elem: string, index: number): void => {
        if (savedFilters.includes(elem)) {
          idxMap[index] = true;
        }
      });
    };

    // parse publication type
    const parsePublicationType = (pubType: string) => {
      return metadata_utils.prettyPrintPublicationType(pubType);
    };

    /**
     * Manage Dialogs.
     */
    const dialogStore = useDialogsStore();

    const closeBookmarkOverview = () => {
      dialogStore.bookmarkOverviewActivated = false;
    };
    const closeDashboard = () => {
      dialogStore.dashboardViewActivated = false;
    };

    const closeTemplateOverview = () => {
      dialogStore.templateOverviewActivated = false;
    };

    const openBookmarkSaveDialog = () => {
      dialogStore.bookmarkSaveActivated = true;
    };

    const closeBookmarkSaveDialog = () => {
      dialogStore.bookmarkSaveActivated = false;
      bookmarkSaveQueryParameterDialog.value = false;
    };

    const closeGroupDialog = () => {
      dialogStore.groupOverviewActivated = false;
    };

    const newBookmarkId = ref(-1);
    const addBookmarkSuccessful = (bookmarkId: number, bookmarkName: string) => {
      newBookmarkId.value = bookmarkId;
      successMsg.value = "Gespeicherte Suche " +
          "'" + bookmarkName + " (" + bookmarkId + ")'" +
          " erfolgreich hinzugefügt."
      successMsgIsActive.value = true;
      dialogStore.bookmarkOverviewActivated = true;
    };

    const addRightSuccessful = (title: string, handle: string) => {
      successMsg.value = "Rechteinformation erfolgreich für Item " +
          "'" + title + " (" + handle + ")' hinzugefügt.";
      successMsgIsActive.value = true;
    };

    const searchHelpDialog = ref(false);

    const selectedRowColor = (row: any) => {
      if(selectedItems.value[0] !== undefined && selectedItems.value[0] == row.item.handle){
        return { class: "bg-blue-lighten-4"}
      }
    };
    const renderKey = ref(0);
    const dialog = ref(false);

    const openDialog = () => {
      dialog.value = true;
    };

    /**
     * Intercept Sortorder
     */
    const datatableOptions = ref<DataTableOptions>({
      page: 1,
      itemsPerPage: 25,
      sortBy: [
        {
          key: 'handle',
          order: 'desc',
        },
      ],
    });

    const onOptionsUpdate = (newOptions: DataTableOptions) => {
      if (newOptions.sortBy.length == 0){
        // When we end here it is probably the initial load of the site
        return;
      }
      const oldSort = datatableOptions.value.sortBy[0]
      const newSort = newOptions.sortBy[0]

      const sortChanged =
          !oldSort ||
          !newSort ||
          oldSort.key !== newSort.key ||
          oldSort.order !== newSort.order

      datatableOptions.value = newOptions

      if (sortChanged) {
        startSearch();
      }
    }

    // Reset the search filter
    const resetFilter: () => void = () => {
      searchStore.publicationYearFrom = "";
      searchStore.publicationYearTo = "";

      searchStore.accessStateOpen = false;
      searchStore.accessStateRestricted = false;
      searchStore.accessStateClosed = false;

      searchStore.accessStateClosed = false;
      searchStore.accessStateOpen = false;
      searchStore.accessStateRestricted = false;

      searchStore.formalRuleLicenceContract = false;
      searchStore.formalRuleCCNoRestriction = false;
      searchStore.formalRuleNoLegalRisk = false;
      searchStore.formalRuleUserAgreement = false;

      searchStore.temporalValidOnFormatted = "";
      searchStore.temporalEventState.startDateOrEndDateFormattedValue = "";
      searchStore.temporalEventState.startDateOrEndDateOption = "";

      searchStore.accessStateIdx = searchStore.accessStateIdx.map(() => false);
      searchStore.paketSigelIdIdx = searchStore.paketSigelIdIdx.map(
          () => false,
      );
      searchStore.publicationTypeIdx = searchStore.publicationTypeIdx.map(
          () => false,
      );
      searchStore.templateNameIdx = searchStore.templateNameIdx.map(
          () => false,
      );
      searchStore.zdbIdIdx = searchStore.zdbIdIdx.map(() => false);
      searchStore.seriesIdx = searchStore.seriesIdx.map(() => false);
      searchStore.licenceUrlIdx = searchStore.licenceUrlIdx.map(() => false);
      searchStore.noRightInformation = false;
      searchStore.manualRight = false;
      searchStore.deletions = false;
      searchStore.accessStateOnDateState.dateValueFormatted = "";
      searchStore.accessStateOnDateState.accessState = "";
      searchStore.accessStateOnDateIdx = [] as Array<string>;
      url.removeQueryParameters(
          route,
          router,
          [
            url.QUERY_PARAMETER_EXECUTE_BOOKMARK_ID,
            url.QUERY_PARAMETER_TEMPLATE_ID,
            url.QUERY_PARAMETER_BOOKMARK_ID,
            url.QUERY_PARAMETER_DASHBOARD_HANDLE_SEARCH,
            url.QUERY_PARAMETER_GROUP_ID,
            url.QUERY_PARAMETER_RIGHT_ID,
          ]
      );
      startEmptySearch();
    };
    const canReset = computed(() => {
      return (
          searchStore.publicationYearFrom != "" ||
          searchStore.publicationYearTo != "" ||
          searchStore.accessStateOpen ||
          searchStore.accessStateRestricted ||
          searchStore.accessStateClosed ||
          searchStore.temporalEventState.startDateOrEndDateFormattedValue != "" ||
          searchStore.temporalEventState.startDateOrEndDateOption != "" ||
          searchStore.accessStateClosed ||
          searchStore.accessStateOpen ||
          searchStore.accessStateRestricted ||
          searchStore.formalRuleLicenceContract ||
          searchStore.formalRuleCCNoRestriction ||
          searchStore.formalRuleNoLegalRisk ||
          searchStore.formalRuleUserAgreement ||
          searchStore.temporalValidOnFormatted != "" ||
          searchStore.accessStateIdx.filter((element) => element).length > 0 ||
          searchStore.paketSigelIdIdx.filter((element) => element).length > 0 ||
          searchStore.zdbIdIdx.filter((element) => element).length > 0 ||
          searchStore.seriesIdx.filter((element) => element).length > 0 ||
          searchStore.templateNameIdx.filter((element) => element).length > 0 ||
          searchStore.publicationTypeIdx.filter((element) => element).length > 0 ||
          searchStore.licenceUrlIdx.filter((element) => element).length > 0 ||
          searchStore.noRightInformation ||
          searchStore.searchTerm ||
          searchStore.isLastSearchForTemplates ||
          searchStore.manualRight ||
          searchStore.deletions ||
          searchStore.accessStateOnDateState.dateValueFormatted ||
          searchStore.accessStateOnDateState.accessState
      );
    });

    return {
      successMsgIsActive,
      successMsg,
      bookmarkSaveQueryParameterDialog,
      dialog,
      errorMsgIsActive,
      errorMsg,
      currentItem,
      currentPage,
      dialogStore,
      groupEditActivated,
      headers,
      headersValueVSelect,
      items,
      newBookmarkId,
      numberOfResults,
      pageSize,
      pageSizes,
      queryParameterBookmark,
      queryParameterGroup,
      queryParameterRight,
      searchStore,
      selectedHeaders,
      selectedItems,
      searchHelpDialog,
      tableContentLoading,
      userStore,
      rightEditActivated,
      templateLoadError,
      templateLoadErrorMsg,
      totalPages,
      renderKey,
      // Methods
      addActiveItem,
      addBookmarkSuccessful,
      canReset,
      closeBookmarkOverview,
      closeBookmarkSaveDialog,
      closeDashboard,
      closeGroupDialog,
      closeGroupEditDialog,
      closeTemplateEditDialog,
      closeTemplateOverview,
      executeBookmarkSearch,
      getAccessStatesForDate,
      initSearchByRightId,
      handlePageChange,
      handlePageSizeChange,
      loadTemplateView,
      onOptionsUpdate,
      openBookmarkSaveDialog,
      openDialog,
      parsePublicationType,
      resetFilter,
      addRightSuccessful,
      searchQuery,
      startDashboardSearch,
      selectedRowColor,
      setActiveItem,
      startEmptySearch,
      startSearch,
    };
  },
});
</script>

<style scoped>
:deep(tr.v-data-table__selected) {
  background: #7d92f5 !important;
}
table.special, th.special, td.special {
  border:1px solid black;
}
* {
  transform: scale(0.98, 0.98)
}
</style>
<template>
  <TopNavigationBar></TopNavigationBar>
  <VResizeDrawer permanent width="300px">
    <SearchFilter
        v-on:startSearch="startSearch"
        v-on:getAccessStatesOnDate="getAccessStatesForDate"
    ></SearchFilter>
  </VResizeDrawer>
  <v-main class="d-flex align-center justify-center">
    <v-dialog
        v-model="dialogStore.bookmarkSaveActivated"
        :retain-focus="false"
        max-width="1000px"
        v-on:close="closeBookmarkSaveDialog"
        persistent
    >
      <BookmarkSave
          :isNew="true"
          :searchTerm="searchStore.searchTerm"
          v-on:addBookmarkSuccessful="addBookmarkSuccessful"
      ></BookmarkSave>
    </v-dialog>
    <ResizableDialog
        v-model="dialogStore.templateOverviewActivated"
        :initial-width="1800"
        :initial-height="900"
        persistent
        @close="closeTemplateOverview"
    >
      <TemplateOverview
          @getItemsByRightId="initSearchByRightId"
          @templateOverviewClosed="closeTemplateOverview"
      ></TemplateOverview>
    </ResizableDialog>

    <ResizableDialog
        v-model="dialogStore.bookmarkOverviewActivated"
        :initial-width="1800"
        :initial-height="900"
        persistent
        @close="closeBookmarkOverview"
    >
      <BookmarkOverview
          @executeBookmarkSearch="executeBookmarkSearch"
          @bookmarkOverviewClosed="closeBookmarkOverview"
      />
    </ResizableDialog>
    <ResizableDialog
        v-model="dialogStore.dashboardViewActivated"
        :initial-width="1800"
        :initial-height="900"
        persistent
        @close="closeDashboard"
    >
      <Dashboard
          v-on:dashboardClosed="closeDashboard"
      ></Dashboard>
    </ResizableDialog>
    <ResizableDialog
        v-model="dialogStore.groupOverviewActivated"
        :initial-width="1800"
        :initial-height="900"
        persistent
        @close="closeDashboard"
    >
      <GroupOverview
          v-on:groupOverviewClosed="closeGroupDialog">
      </GroupOverview>
    </ResizableDialog>
    <v-dialog v-model="templateLoadError" max-width="1000">
      <v-card>
        <v-card-title class="text-h5"
        >Laden von Template fehlgeschlagen</v-card-title
        >
        <v-card-text>
          Informationen zum Fehler: {{ templateLoadErrorMsg }}
        </v-card-text>
      </v-card>
    </v-dialog>
    <v-dialog
        v-model="rightEditActivated"
        :retain-focus="false"
        max-width="1500px"
        max-height="850px"
        v-on:close="closeTemplateEditDialog"
        persistent
    >
      <RightsEditDialog
          :index="-1"
          :isNewRight="false"
          :isNewTemplate="false"
          :rightId="queryParameterRight.rightId"
          v-on:editRightClosed="closeTemplateEditDialog"
      ></RightsEditDialog>
    </v-dialog>
    <v-dialog
        v-model="groupEditActivated"
        :retain-focus="false"
        v-on:close="closeGroupEditDialog"
        max-width="1500px"
        max-height="850px"
        scrollable
        persistent
    >
      <GroupEdit
          :isNew="false"
          :group="queryParameterGroup"
          v-on:groupEditClosed="closeGroupEditDialog"
      ></GroupEdit>
    </v-dialog>
    <v-dialog
        v-model="bookmarkSaveQueryParameterDialog"
        :retain-focus="false"
        v-on:close="closeBookmarkSaveDialog"
        max-width="1000px"
        persistent
    >
      <BookmarkSave
          :isNew="false"
          :bookmark="queryParameterBookmark"
          v-on:closeEditDialog="closeBookmarkSaveDialog"
      ></BookmarkSave>
    </v-dialog>
    <v-card position="relative">
      <v-card-title>
        <v-text-field
            v-model="searchStore.searchTerm"
            append-icon="mdi-magnify"
            clearable
            label="Suche"
            variant="outlined"
            single-line
            @click:append="startSearch"
            @keydown.enter.prevent="startSearch"
        ></v-text-field>
      </v-card-title>
      <v-row
          no-gutters
          justify="space-around"
      >
        <v-col
        >
          <b>Aktive Filter:</b> {{ searchStore.filtersAsQuery }}
        </v-col>
      </v-row>
      <v-row justify="end" align="center" class="ga-0">
        <v-col
            cols="auto"
        >
          <v-dialog v-model="searchHelpDialog" max-width="600px">
            <template v-slot:activator="{ props: activatorProps }">
              <v-tooltip location="bottom" text="Syntax der Sucheingabe">
                <template v-slot:activator="{ props }">
                  <v-btn
                      density="compact"
                      icon="mdi-help"
                      v-bind="{...activatorProps, ...props}"
                  >
                  </v-btn>
                </template>
              </v-tooltip>
            </template>
            <v-card
            >
              <template v-slot:actions>
                <v-spacer></v-spacer>
                <v-btn color="blue darken-1" @click="searchHelpDialog = false"> Zurück </v-btn>
              </template>
              <v-card-title class="text-h5">
                Syntax der Sucheingabe
              </v-card-title>
              <v-card-text>
                <p class="text-left text-body-1 font-weight-bold">Genereller Aufbau:</p>
                <p class="text-center text-body-2 bg-grey-lighten-2">
                  suchschluessel1:"wert1" & suchschluessel2:"wert2"
                </p>
                <p class="text-left text-body-2 mt-1 mb-1">
                  Das " kann auch durch ' ersetzt werden. Beides kann man
                  weglassen wenn nur ein Wort gesucht wird.
                </p>
                <table class="special">
                  <thead>
                  <tr class=special>
                    <th class=special>Suche</th>
                    <th class=special>Suchschlüssel</th>
                    <th class=special>Format</th>
                  </tr>
                  </thead>
                  <tbody>
                  <tr class=special>
                    <td class=special>Titel</td>
                    <td class=special>tit</td>
                    <td class=special></td>
                  </tr>
                  <tr class=special>
                    <td class=special>Handle des Items</td>
                    <td class=special>hdl</td>
                    <td class=special></td>
                  </tr>
                  <tr class=special>
                    <td class=special>Community</td>
                    <td class=special>com</td>
                    <td class=special></td>
                  </tr>
                  <tr class=special>
                    <td class=special>Handle Community</td>
                    <td class=special>hdlcom</td>
                    <td class=special></td>
                  </tr>
                  <tr class=special>
                    <td class=special>Handle Subommunity</td>
                    <td class=special>hdlsubcom</td>
                    <td class=special></td>
                  </tr>
                  <tr class=special>
                    <td class=special>Collection</td>
                    <td class=special>col</td>
                    <td class=special></td>
                  </tr>
                  <tr class=special>
                    <td class=special>Handle Collection</td>
                    <td class=special>hdlcol</td>
                    <td class=special></td>
                  </tr>
                  <tr class=special>
                    <td class=special>ZDB-Id</td>
                    <td class=special>zdb</td>
                    <td class=special></td>
                  </tr>
                  <tr class=special>
                    <td class=special>Paket-Sigel</td>
                    <td class=special>sig</td>
                    <td class=special></td>
                  </tr>
                  <tr class=special>
                    <td class=special>Lizenz URL</td>
                    <td class=special>lur</td>
                    <td class=special></td>
                  </tr>
                  <tr class=special>
                    <td class=special>Lizenz URL ohne Protokolle, Punkte und Slashes</td>
                    <td class=special>luk</td>
                    <td class=special></td>
                  </tr>
                  <tr class=special>
                    <td class=special>Series</td>
                    <td class=special>ser</td>
                    <td class=special></td>
                  </tr>
                  <tr class=special>
                    <td class=special>Template Namen</td>
                    <td class=special>tpl</td>
                    <td class=special></td>
                  </tr>
                  <tr class=special>
                    <td class=special>Publikationsjahr</td>
                    <td class=special>jah</td>
                    <td class=special>Beginn-Ende</td>
                  </tr>
                  <tr class=special>
                    <td class=special>Publikationstyp</td>
                    <td class=special>typ</td>
                    <td class=special></td>
                  </tr>
                  <tr class=special>
                    <td class=special>Access</td>
                    <td class=special>acc</td>
                    <td class=special></td>
                  </tr>
                  <tr class=special>
                    <td class=special>Zeitliche Gültigkeit am</td>
                    <td class=special>zgp</td>
                    <td class=special>YYYY-MM-DD</td>
                  </tr>
                  <tr class=special>
                    <td class=special>Zeitliche Gültigkeit Beginn</td>
                    <td class=special>zgb</td>
                    <td class=special>YYYY-MM-DD</td>
                  </tr>
                  <tr class=special>
                    <td class=special>Zeitliche Gültigkeit Ende</td>
                    <td class=special>zge</td>
                    <td class=special>YYYY-MM-DD</td>
                  </tr>
                  <tr class=special>
                    <td class=special>Formale Regelungen</td>
                    <td class=special>reg</td>
                    <td class=special></td>
                  </tr>
                  <tr class=special>
                    <td class=special>Keine Rechteeinträge</td>
                    <td class=special>nor</td>
                    <td class=special>nor:on</td>
                  </tr>
                  <tr class=special>
                    <td class=special>Access-Status am</td>
                    <td class=special>acd</td>
                    <td class=special>STATUS+YYYY-MM-DD</td>
                  </tr>
                  <tr class=special>
                    <td class=special>ISBN</td>
                    <td class=special>isb</td>
                    <td class=special></td>
                  </tr>
                  <tr class=special>
                    <td class=special>DOI</td>
                    <td class=special>doi</td>
                    <td class=special></td>
                  </tr>
                  <tr class=special>
                    <td class=special>PPN</td>
                    <td class=special>ppn</td>
                    <td class=special></td>
                  </tr>
                  <tr class=special>
                    <td class=special>Manuell erstellte Rechteeinträge</td>
                    <td class=special>man</td>
                    <td class=special>man:on</td>
                  </tr>
                  <tr class=special>
                    <td class=special>Löschungen</td>
                    <td class=special>del</td>
                    <td class=special>del:on</td>
                  </tr>
                  </tbody>
                </table>

                <p class="text-left text-body-1 mt-4 font-weight-bold">
                  Bool'sche Operatoren
                </p>
                <table class="special">
                  <thead>
                  <tr class=special>
                    <th class=special> </th>
                    <th class=special> </th>
                    <th class=special>Beispiele</th>
                  </tr>
                  </thead>
                  <tbody>
                  <tr class=special>
                    <td class=special>Und</td>
                    <td class=special>&</td>
                    <td class=special>col:'Economics & series'</td>
                  </tr>
                  <tr class=special>
                    <td class=special>Nicht</td>
                    <td class=special>!</td>
                    <td class=special>
                      !hdl:'1234' <br>
                      !(hdl:'1234' | tit:'geopolitical') <br>
                      col:'department' & !tit:'geopolitical'
                    </td>
                  </tr>
                  <tr class=special>
                    <td class=special>Oder</td>
                    <td class=special>|</td>
                    <td class=special>col:'Economics' | ser:'some series'</td>
                  </tr>
                  </tbody>
                </table>

                <p class="text-left text-body-2 mt-1 mb-1">
                  Es ist möglich die verschiedenen Operatoren in einem Term
                  zu verwenden und mittels Klammern zu strukturieren.
                </p>

                <p class="text-center text-body-2 bg-grey-lighten-2 mt-1 mb-1">
                  col:"subject1" | (hdl:"handle" & !com:"community")
                </p>
                <p class="text-left text-body-1 mt-4 font-weight-bold">Sonderzeichen</p>

                <p class="text-left text-body-2 mt-1 mb-1">
                  Rechtstrunkierung ist möglich für alle Textbasierten Suchwerte mit *, z.B:
                </p>
                <p class="text-center text-body-2 bg-grey-lighten-2 mt-1 mb-1">
                  lur:'by-nc-nd*'
                </p>

                <p class="text-left text-body-1 mt-4 font-weight-bold">Suche von mehreren Werten</p>

                <p class="text-left text-body-2 mt-1 mb-1">
                  Für folgende Suchschlüssel können mehrere Werte mit einem Suchschlüssel eingegeben werden: <b>doi</b>,<b>hdl</b>,<b>isb</b>,<b>ppn</b>,<b>sig</b>,<b>zdb</b>.
                  <br>
                  <b>Wichtig</b>: Wildcards funktionieren nicht mit dieser Syntax!
                </p>

                <p class="text-center text-body-2 bg-grey-lighten-2 mt-1 mb-1">
                  hdl:"11159/1234,11159/5678"
                </p>
                <p class="text-left text-body-2 mt-1 mb-1">
                  Alternative Suche, die Wildcard-Verwendung ermöglicht:
                </p>
                <p class="text-center text-body-2 bg-grey-lighten-2 mt-1 mb-1">
                  hdl:"11159/1234,11159/5678" | hdl:"11159/555*"
                </p>
              </v-card-text>
            </v-card>
          </v-dialog>
        </v-col>
        <v-col
            cols="auto">
          <v-btn
              color="blue darken-1"
          >
            Exportieren
          </v-btn>
        </v-col>
        <v-col
          cols="auto">
          <v-btn
              color="blue darken-1"
              @click="openBookmarkSaveDialog"
              :disabled="!userStore.isLoggedIn || !searchStore.isLastSearchNonTrivialAndSuccessful"
          >
            Speichern
          </v-btn>
        </v-col>
        <v-col
          cols="auto">
          <v-btn
              color="blue darken-1"
              :disabled="!canReset"
              @click="resetFilter"
          >
            Resetten</v-btn
          >
        </v-col>
      </v-row>
      <v-spacer></v-spacer>
      <v-snackbar
          multi-line
          location="bottom"
          timer="true"
          timeout="5000"
          v-model="errorMsgIsActive"
          color="error"
      >
        {{ errorMsg }}
      </v-snackbar>
      <v-snackbar
          multi-line
          location="bottom"
          timer="true"
          timeout="5000"
          v-model="successMsgIsActive"
          color="success"
      >
        {{ successMsg }}
      </v-snackbar>

      <v-select
          v-model="headersValueVSelect"
          :items="headers"
          label="Spaltenauswahl"
          multiple
          return-object
      >
        <template v-slot:selection="{ item, index }">
          <v-chip v-if="index === 0">
            <span>{{ item.title }}</span>
          </v-chip>
          <span v-if="index === 1" class="grey--text caption"
          >(+{{ headersValueVSelect.length - 1 }} weitere)</span
          >
        </template>
      </v-select>

      <v-col cols="5" sm="5"> Suchergebnisse: {{ numberOfResults }}</v-col>
      <v-data-table
          v-model="selectedItems"
          :headers="selectedHeaders"
          :items="items.map((value) => value.metadata)"
          :items-per-page="0"
          item-value="handle"
          :loading="tableContentLoading"
          :key="renderKey"
          :row-props="selectedRowColor"
          loading-text="Daten werden geladen... Bitte warten."
          select-strategy="single"
          height="550px"
          @click:row="addActiveItem"
          @dblclick:row="setActiveItem"
          @update:options="onOptionsUpdate"
      >
        <template v-slot:item.title="{ item }">
          <td v-if="item.deleted">❌{{item.title}} </td>
          <td v-else>{{item.title}} </td>
        </template>
        <template v-slot:item.paketSigel="{ item }">
          <td>
            {{ item.paketSigel?.join() }}
          </td>
        </template>
        <template v-slot:item.isPartOfSeries="{ item }">
          <td>
            {{ item.isPartOfSeries?.join() }}
          </td>
        </template>
        <template v-slot:item.isbn="{ item }">
          <td>
            {{ item.isbn?.join() }}
          </td>
        </template>
        <template v-slot:item.handle="{ item }">
          <td>
            <a
                v-bind:href="
                    metadata_utils.hrefHandle(
                      item.handle,
                      searchStore.handleURLResolver,
                    )
                  "
                target="_blank"
            >{{ metadata_utils.shortenHandle(item.handle) }}</a
            >
          </td>
        </template>
        <template v-slot:item.publicationType="{ item }">
          <td>{{ parsePublicationType(item.publicationType) }}</td>
        </template>
        <template #bottom></template>
      </v-data-table>
      <v-col cols="14" sm="12">
        <v-row>
          <v-col cols="2" sm="2">
            <v-select
                v-model="pageSize"
                :items="pageSizes"
                label="Einträge pro Seite"
            ></v-select>
          </v-col>
          <v-col cols="10" sm="9">
            <v-pagination
                v-model="currentPage"
                :length="totalPages"
                next-icon="mdi-menu-right"
                prev-icon="mdi-menu-left"
                total-visible="7"
            ></v-pagination>
          </v-col>
        </v-row>
      </v-col>
    </v-card>
  </v-main>
  <VResizeDrawer location="right" width="400px" permanent>
    <v-card v-if="currentItem.metadata" class="mx-auto" tile>
      <RightsView
          :handle="currentItem.metadata.handle"
          :rights="currentItem.rights"
          :title="currentItem.metadata.title"
          :licenceUrl="currentItem.metadata.licenceUrl"
          v-on:addRightSuccessful="addRightSuccessful"
      ></RightsView>
      <MetadataView
          :metadata="Object.assign({}, currentItem.metadata)"
      ></MetadataView>
    </v-card>
  </VResizeDrawer>
</template>
