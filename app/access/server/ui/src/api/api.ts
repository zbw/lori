import {
  AccessInformation,
  AccessinformationApi,
  Configuration,
} from "@/generated-sources/openapi";

import { ResultAsync } from "neverthrow";

const configuration = new Configuration({
  basePath: window.location.origin + "/api/v1",
});

const accessInformationApi = new AccessinformationApi(configuration);

export default {
  getList(offset: number, limit: number): Promise<Array<AccessInformation>> {
    return accessInformationApi.getAccessInformationList({
      offset: offset,
      limit: limit,
    });
  },
  getItemById(id: string): ResultAsync<AccessInformation, Error> {
    return ResultAsync.fromPromise(
      accessInformationApi.getAccessInformationByIds({
        id: id,
      }),
      (e: unknown) => {
        const errResponse = e as Response;
        return new Error(
          "Getting an entry resulted in following error:\n" +
            errResponse.status +
            ": " +
            errResponse.statusText
        );
      }
    );
  },
  deleteAccessInformation(itemId: string): ResultAsync<void, Error> {
    return ResultAsync.fromPromise(
      accessInformationApi.deleteAccessInformationById({ id: itemId }),
      (e: unknown) => {
        const errResponse = e as Response;
        return new Error(
          "Deleting an entry resulted in following error:\n" +
            errResponse.status +
            ": " +
            errResponse.statusText
        );
      }
    );
  },
};
