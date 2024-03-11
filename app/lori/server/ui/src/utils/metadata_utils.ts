const HANDLE_URL = "https://hdl.handle.net/";
export default {
  shortenHandle(handle: string): string {
    if (handle.startsWith("http")) {
      return handle.substring(22);
    } else {
      return handle;
    }
  },

  hrefHandle(handle: string): string {
    if (handle.startsWith("https")) {
      return handle;
    }
    if (handle.startsWith("http")) {
      return "https" + handle.substring(4);
    }
    return HANDLE_URL + handle;
  },

  prependHandleUrl(handlePath: string | undefined) : string {
    if (handlePath == undefined){
      return HANDLE_URL;
    }
    return HANDLE_URL + handlePath;
  },
};
