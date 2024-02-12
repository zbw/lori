export default {
  /**
   * ISO 8601: YYYY-MM-DD
   */
  dateToIso8601(date: Date): string {
    const offset = date.getTimezoneOffset();
    return new Date(date.getTime() - offset * 60 * 1000)
      .toISOString()
      .split("T")[0];
  },

  isEmpty(obj: object): boolean {
    for (const prop in obj) {
      if (Object.hasOwn(obj, prop)) {
        return false;
      }
    }
    return true;
  },

  isEmptyObject(obj: object | undefined): boolean {
    if (obj == undefined) {
      return false;
    }
    const proto = Object.getPrototypeOf(obj);
    if (proto !== null && proto !== Object.prototype) {
      return false;
    }
    return this.isEmpty(obj);
  },
};
