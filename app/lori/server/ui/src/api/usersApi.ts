import {
  Configuration,
  SessionIdCreated,
  SessionRest,
  UsersApi,
} from "@/generated-sources/openapi";

const configuration = new Configuration({
  basePath: window.location.origin + "/api/v1",
});
const usersApi = new UsersApi(configuration);

export default {
  addSession(session: SessionRest): Promise<SessionIdCreated> {
    return usersApi.addSession({ body: session });
  },
  deleteSessionById(sessionId: string): Promise<void> {
    return usersApi.deleteSessionById({
      id: sessionId,
    });
  },
  getSessionById(sessionId: string): Promise<SessionRest> {
    return usersApi.getSessionByID({
      id: sessionId,
    });
  },
};
