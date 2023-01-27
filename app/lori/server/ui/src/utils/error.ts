import { ErrorRest } from "@/generated-sources/openapi";

export default {
  createErrorMsg(error: ErrorRest): string {
    return (
      error.title + ": " + error.detail + " (Status: " + error.status + ")"
    );
  },
};
