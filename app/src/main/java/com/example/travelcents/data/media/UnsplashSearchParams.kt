package com.example.travelcents.data.media

data class UnsplashSearchParams(
    val orientation: String? = "squarish",
    val color: String? = null,
    val orderBy: String = "relevant",
    val contentFilter: String = "high",
    val perPage: Int = 5,
    val pageIndex: Int = 1
) {
    fun sanitized(): UnsplashSearchParams = copy(
        perPage = perPage.coerceIn(1, 30),
        pageIndex = pageIndex.coerceAtLeast(1),
        orientation = orientation?.trim()?.takeIf { it.isNotEmpty() },
        color = color?.trim()?.takeIf { it.isNotEmpty() },
        orderBy = orderBy.trim().ifEmpty { "relevant" },
        contentFilter = contentFilter.trim().ifEmpty { "high" }
    )
}
