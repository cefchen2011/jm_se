package com.comicreader.data

import com.google.gson.JsonObject
import retrofit2.http.GET
import retrofit2.http.Query

interface JmApi {

    @GET("latest")
    suspend fun latest(@Query("page") page: Int): JsonObject

    @GET("search")
    suspend fun search(
        @Query("search_query") keyword: String,
        @Query("o") order: String,
        @Query("page") page: Int
    ): JsonObject

    @GET("categories")
    suspend fun categories(): JsonObject

    @GET("categories/filter")
    suspend fun categoriesFilter(
        @Query("page") page: Int,
        @Query("o") order: String,
        @Query("c") slug: String
    ): JsonObject

    @GET("hot_tags")
    suspend fun hotTags(): JsonObject

    @GET("album")
    suspend fun album(@Query("id") id: String): JsonObject

    @GET("comic_read")
    suspend fun comicRead(@Query("id") id: String): JsonObject

    @GET("random_recommend")
    suspend fun randomRecommend(): JsonObject
}
