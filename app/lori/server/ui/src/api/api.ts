import { Configuration, ItemApi, ItemRest } from "@/generated-sources/openapi";

import { ResultAsync } from "neverthrow";

const configuration = new Configuration({
  basePath: window.location.origin + "/api/v1",
});

const accessInformationApi = new ItemApi(configuration);

export default {
  getList(offset: number, limit: number): Promise<Array<ItemRest>> {
    return accessInformationApi.getItemList({
      offset: offset,
      limit: limit,
    });
  },
  getItemById(id: string): ResultAsync<ItemRest, Error> {
    return ResultAsync.fromPromise(
      accessInformationApi.getItemByIds({
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
};
