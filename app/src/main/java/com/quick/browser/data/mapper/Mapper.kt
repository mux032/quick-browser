package com.quick.browser.data.mapper

import com.quick.browser.data.local.entity.SavedArticle as SavedArticleEntity
import com.quick.browser.data.local.entity.Tag as TagEntity
import com.quick.browser.domain.model.SavedArticle
import com.quick.browser.domain.model.Tag

fun SavedArticleEntity.toDomain(): SavedArticle {
    return SavedArticle(
        url = url,
        title = title,
        content = content,
        author = byline,
        siteName = siteName,
        publishDate = publishDate,
        savedDate = savedDate,
        excerpt = excerpt
    )
}

fun SavedArticle.toEntity(): SavedArticleEntity {
    return SavedArticleEntity(
        url = url,
        title = title,
        content = content,
        byline = author,
        siteName = siteName,
        publishDate = publishDate,
        savedDate = savedDate,
        excerpt = excerpt
    )
}

fun TagEntity.toDomain(): Tag {
    return Tag(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Tag.toEntity(): TagEntity {
    return TagEntity(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}