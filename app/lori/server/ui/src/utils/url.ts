import {NavigationFailure, RouteLocationNormalizedLoaded, Router} from "vue-router";

const QUERY_PARAMETER_BOOKMARK_ID = "bookmarkId";
const QUERY_PARAMETER_DASHBOARD_HANDLE_SEARCH = "dashboardHandleSearch";
const QUERY_PARAMETER_EXECUTE_BOOKMARK_ID = "executeBookmarkId";
const QUERY_PARAMETER_GROUP_ID = "groupId";
const QUERY_PARAMETER_HANDLE = "handle";
const QUERY_PARAMETER_RIGHT_ID = "rightId";
const QUERY_PARAMETER_TEMPLATE_ID = "templateId";
const QUERY_PARAMETER_SEARCH = "executeSearch";
const REST_API_BASE_PATH = "/api/v1";
const REST_API_DOWNLOAD_PATH = "/download";

export default {
    QUERY_PARAMETER_BOOKMARK_ID,
    QUERY_PARAMETER_DASHBOARD_HANDLE_SEARCH,
    QUERY_PARAMETER_EXECUTE_BOOKMARK_ID,
    QUERY_PARAMETER_GROUP_ID,
    QUERY_PARAMETER_HANDLE,
    QUERY_PARAMETER_RIGHT_ID,
    QUERY_PARAMETER_SEARCH,
    QUERY_PARAMETER_TEMPLATE_ID,
    REST_API_BASE_PATH,
    REST_API_DOWNLOAD_PATH,

    addQueryParameters(
        route: RouteLocationNormalizedLoaded,
        router: Router,
        paramsToAdd: Record<string, string | number | boolean | null | undefined>
    ): void {
        const newQuery = { ...route.query };
        let shouldReplace = false;

        for (const [key, value] of Object.entries(paramsToAdd)) {
            if (value !== undefined && value !== null) {
                if (newQuery[key] !== value.toString()) {
                    newQuery[key] = value.toString();
                    shouldReplace = true;
                }
            }
        }

        if (shouldReplace) {
            router.replace({ query: newQuery })
                .catch((e: NavigationFailure) => {
                    console.error('Navigation failed:', e);
                });
        }
    },

    removeQueryParameters(
        route: RouteLocationNormalizedLoaded,
        router: Router,
        paramsToRemove: string[]
    ) : void {
        const newQuery = { ...route.query };
        let shouldReplace = false;

        for (const param of paramsToRemove) {
            if (param in newQuery) {
                delete newQuery[param];
                shouldReplace = true;
            }
        }

        if (shouldReplace) {
            router.replace({ query: newQuery })
                .catch((e: NavigationFailure) => {
                        console.error('Navigation failed:', e);
                    }
                );
        }
    },

    getQueryParameter(
        parameter: string,
    ): string | null {
        const urlParams = new URLSearchParams(window.location.search);
        return urlParams.get(parameter);
    },

    createTemplateHref(rightId: string | undefined) : string {
        if(rightId == undefined){
            return "";
        } else {
            return window.location.origin + window.location.pathname + "?" +
                QUERY_PARAMETER_TEMPLATE_ID + "=" + rightId;
        }
    },
    createExecuteBookmarkHref(bookmarkId: number) : string {
        return window.location.origin + window.location.pathname + "?" +
            QUERY_PARAMETER_EXECUTE_BOOKMARK_ID + "=" + bookmarkId;
    },
    createHandleHref (handleId : string) : string {
        return window.location.origin + window.location.pathname + "?" +
            QUERY_PARAMETER_DASHBOARD_HANDLE_SEARCH + "=hdl:" + handleId;
    },
    createDownloadHref(downloadId: string) : string {
        return window.location.origin + REST_API_BASE_PATH + REST_API_DOWNLOAD_PATH + "/" + downloadId;
    }
}