export default {
  haveSameKeys<T>(a: T[], b: T[], key: keyof T): boolean {
    const getKeySet = (arr: T[]) => new Set(arr.map((item) => item[key]));

    const setA = getKeySet(a);
    const setB = getKeySet(b);

    if (setA.size !== setB.size) return false;

    for (const value of setA) {
      if (!setB.has(value)) return false;
    }

    return true;
  },
};
