package com.aethertv.v2.data

enum class ContentType {
    LIVE,
    MOVIE,
    SERIES
}

data class CategoryItem(
    val id: String,
    val name: String
)

data class MediaItem(
    val id: String,
    val name: String,
    val group: String,
    val posterUrl: String?,
    val streamUrl: String,
    val contentType: ContentType,
    val seriesId: String? = null,
    val badge: String = ""
)

data class MediaDetailInfo(
    val plot: String = "",
    val genre: String = "",
    val releaseYear: String = "",
    val rating: String = "",
    val duration: String = "",
    val backdropUrl: String? = null
)

data class EpisodeItem(
    val id: String,
    val season: String,
    val title: String,
    val streamUrl: String
)
