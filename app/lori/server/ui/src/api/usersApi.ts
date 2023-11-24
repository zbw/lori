import {
  Configuration,
  UsersApi,
  UserSessionRest,
} from "@/generated-sources/openapi";

const configuration = new Configuration({
  basePath: window.location.origin + "/api/v1",
});
const usersApi = new UsersApi(configuration);

export default {
  getSessionById(jsessionID: string): Promise<UserSessionRest> {
    return usersApi.getUserSession({
      headers: { Cookie: "JSESSIONID=" + jsessionID },
    });
  },
  deleteSessionById(jsessionID: string): Promise<void> {
    return usersApi.deleteSession({
      headers: { Cookie: "JSESSIONID=" + jsessionID },
    });
  },
};
