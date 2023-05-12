import {ErrorRest} from "@/generated-sources/openapi";

const unexpectedError =
    "Ein unerwarteter Fehler ist aufgetreten. Die Fehlernachricht vom Backend kann nicht gelesen werden.";
export default {
    createErrorMsg(error: ErrorRest): string {
        return (
            error.title + ": " + error.detail + " (Status: " + error.status + ")"
        );
    },
    errorHandling(e: any, callback: (errorMsg: string) => void) {
        try {
            e.response
                .json()
                .then((err: ErrorRest) => {
                    callback(this.createErrorMsg(err));
                })
                .catch((e: any) => {
                    callback(unexpectedError + "; " + e.toString());
                });
        } catch (e: any) {
            callback(unexpectedError + "; " + e.toString());
        }
    },
};
