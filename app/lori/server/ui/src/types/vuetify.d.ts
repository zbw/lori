export type SortItem = {
  key: string;
  order?: "asc" | "desc";
};

interface DataTableOptions {
  page: number;
  itemsPerPage: number;
  sortBy: SortItem[];
}

import type { VDataTable } from "vuetify/components";

// Utility to extract header type from VDataTable props
export type ReadonlyHeaders = VDataTable["$props"]["headers"];
export type UnwrapReadonlyArray<A> =
  A extends Readonly<Array<infer I>> ? I : never;
export type ReadonlyDataTableHeader = UnwrapReadonlyArray<ReadonlyHeaders>;
