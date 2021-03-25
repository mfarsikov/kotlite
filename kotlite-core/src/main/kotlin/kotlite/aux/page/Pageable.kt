package kotlite.aux.page

import kotlite.aux.sort.SortCol

data class Page<T>(//TODO check it works from a jar, not from sources
    val pageable: Pageable,
    val content: List<T>,
    val sort: List<SortCol> = emptyList(),
)

data class Pageable(
    /**
     * Zero based page number
     */
    val pageNumber: Int,
    val pageSize: Int,
){
    val offset = pageNumber * pageSize
}