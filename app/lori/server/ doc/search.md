Searching in LoRi
====

LoRi provides a Search interface to search for items that match specific filters and/or
the content of specific fields matches a search-term or is a least similar enough.

## API
`app/lori/api/src/main/openapi/item.api.yaml` lists under the point `item-search` all
possible search parameters that are available. 

## Search-terms
For finding suitable matches for search-terms provided by an end-user we use the postgres module `pg_trgm`
(Documentation can be found here: https://www.postgresql.org/docs/current/pgtrgm.html).
This module allows to create optimized indices for search purposes. Basically, the similarity
between the input and values in the index is computed based on character triples.

## Filter
We decide between two types of filters. Filters which focus on metadata fields and right fields.
The simpler ones are metadata filters. Only items are returned which match these conditions.
Right filter on the other hand filter for items which contain at least on right information matching
the condition.
