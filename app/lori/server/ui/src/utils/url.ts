import {NavigationFailure, RouteLocationNormalizedLoaded, Router} from "vue-router";

const QUERY_PARAMETER_DASHBOARD_HANDLE_SEARCH = "dashboardHandleSearch";
const QUERY_PARAMETER_EXECUTE_BOOKMARK_ID = "executeBookmarkId";
const QUERY_PARAMETER_HANDLE = "handle";
const QUERY_PARAMETER_RIGHT_ID = "rightId";
const QUERY_PARAMETER_TEMPLATE_ID = "templateId";

export default {
    QUERY_PARAMETER_DASHBOARD_HANDLE_SEARCH,
    QUERY_PARAMETER_EXECUTE_BOOKMARK_ID,
    QUERY_PARAMETER_HANDLE,
    QUERY_PARAMETER_RIGHT_ID,
    QUERY_PARAMETER_TEMPLATE_ID,

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
}