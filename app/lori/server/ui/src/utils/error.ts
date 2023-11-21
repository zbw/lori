import { ErrorRest } from "@/generated-sources/openapi";

const unexpectedError =
  "Ein unerwarteter Fehler ist aufgetreten. Die Fehlernachricht vom Backend kann nicht gelesen werden.";
export default {
  createErrorMsg(error: ErrorRest): string {
    return (
      error.title + ": " + error.detail + " (Status: " + error.status + ")"
    );
  },
  createErrorCode(error: ErrorRest): string {
    return error.status == undefined ? "unknown" : error.status;
  },
  createErrorDetail(error: ErrorRest): string {
    return error.detail == undefined ? "no details" : error.detail;
  },
  errorHandling(
    e: any,
    callback: (errorMsg: string, errorCode: string, errorDetail: string) => void
  ) {
    try {
      e.response
        .json()
        .then((err: ErrorRest) => {
          callback(
            this.createErrorMsg(err),
            this.createErrorCode(err),
            this.createErrorDetail(err)
          );
        })
        .catch((e: any) => {
          callback(
            unexpectedError + "; " + e.toString(),
            "No Error code",
            "No Details"
          );
        });
    } catch (e: any) {
      callback(
        unexpectedError + "; " + e.toString(),
        "No Error code",
        "No Details"
      );
    }
  },
};
