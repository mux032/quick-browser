package com.quick.browser.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration script to update the database schema from version 1 to version 2
 *
 * This migration adds the tags and article_tags tables to support the new tagging system.
 */
object Migration1To2 : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create the tags table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `tags` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // Create a unique index on the name column of the tags table
        database.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`)
            """.trimIndent()
        )
        
        // Create the article_tags table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `article_tags` (
                `articleUrl` TEXT NOT NULL,
                `tagId` INTEGER NOT NULL,
                PRIMARY KEY(`articleUrl`, `tagId`),
                FOREIGN KEY(`articleUrl`) REFERENCES `saved_articles`(`url`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        
        // Create indexes for the article_tags table
        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_article_tags_articleUrl` ON `article_tags` (`articleUrl`)
            """.trimIndent()
        )
        
        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_article_tags_tagId` ON `article_tags` (`tagId`)
            """.trimIndent()
        )
    }
}