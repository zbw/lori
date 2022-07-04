import { DataTableHeader } from "vuetify";

export interface ItemSlot {
  expand: (v: boolean) => void;
  index: number;
  item: unknown;
  isExpanded: boolean;
  isMobile: boolean;
  isSelected: boolean;
  select: (v: boolean) => void;
  headers: DataTableHeader[];
}
