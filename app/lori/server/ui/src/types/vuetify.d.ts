export type SortItem = {
    key: string
    order?: 'asc' | 'desc'
}

interface DataTableOptions {
    page: number
    itemsPerPage: number
    sortBy: SortItem[]
}
