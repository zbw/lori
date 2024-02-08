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
};
