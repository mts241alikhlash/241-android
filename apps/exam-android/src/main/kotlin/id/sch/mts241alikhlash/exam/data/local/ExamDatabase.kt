package id.sch.mts241alikhlash.exam.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CachedAnswerEntity::class, CachedQuestionEntity::class, CachedSessionEventEntity::class],
    version = 5,
    exportSchema = true,
)
abstract class ExamDatabase : RoomDatabase() {
    abstract fun answerDao(): AnswerDao

    abstract fun questionDao(): QuestionDao

    abstract fun sessionEventDao(): SessionEventDao

    companion object {
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE `cached_questions` (
                            `sessionId` TEXT NOT NULL,
                            `id` TEXT NOT NULL,
                            `number` INTEGER NOT NULL,
                            `type` TEXT NOT NULL,
                            `bodyHtml` TEXT NOT NULL,
                            `choicesJson` TEXT,
                            `cachedAt` INTEGER NOT NULL,
                            PRIMARY KEY(`sessionId`, `id`)
                        )
                        """.trimIndent(),
                    )
                }
            }

        val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("DELETE FROM `cached_answers` WHERE `isSynced` = 1")
                    db.execSQL("DROP TABLE `cached_questions`")
                    db.execSQL(
                        """
                        CREATE TABLE `cached_questions` (
                            `sessionId` TEXT NOT NULL,
                            `id` TEXT NOT NULL,
                            `number` INTEGER NOT NULL,
                            `type` TEXT NOT NULL,
                            `bodyHtml` TEXT NOT NULL,
                            `points` REAL NOT NULL,
                            `choicesJson` TEXT,
                            `cachedAt` INTEGER NOT NULL,
                            PRIMARY KEY(`sessionId`, `id`)
                        )
                        """.trimIndent(),
                    )
                }
            }

        val MIGRATION_3_4 =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `session_events` (
                            `clientId` TEXT NOT NULL,
                            `sessionId` TEXT NOT NULL,
                            `type` TEXT NOT NULL,
                            `isViolation` INTEGER NOT NULL,
                            `occurredAtMs` INTEGER NOT NULL,
                            `detail` TEXT,
                            `isSynced` INTEGER NOT NULL,
                            PRIMARY KEY(`clientId`)
                        )
                        """.trimIndent(),
                    )
                }
            }

        val MIGRATION_4_5 =
            object : Migration(4, 5) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `cached_answers_new` (
                            `questionId` TEXT NOT NULL,
                            `sessionId` TEXT NOT NULL,
                            `answer` TEXT NOT NULL,
                            `isDoubtful` INTEGER NOT NULL,
                            `clientId` TEXT NOT NULL,
                            `syncState` TEXT NOT NULL,
                            `updatedAtMillis` INTEGER NOT NULL,
                            `confirmedAtMillis` INTEGER,
                            PRIMARY KEY(`questionId`)
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        INSERT INTO `cached_answers_new`
                            (`questionId`, `sessionId`, `answer`, `isDoubtful`, `clientId`,
                             `syncState`, `updatedAtMillis`, `confirmedAtMillis`)
                        SELECT
                            `questionId`,
                            `sessionId`,
                            `answer`,
                            `isDoubtful`,
                            lower(hex(randomblob(16))),
                            CASE WHEN `isSynced` = 1 THEN 'SERVER_CONFIRMED' ELSE 'LOCAL_PENDING' END,
                            `updatedAt`,
                            CASE WHEN `isSynced` = 1 THEN `updatedAt` ELSE NULL END
                        FROM `cached_answers`
                        """.trimIndent(),
                    )
                    db.execSQL("DROP TABLE `cached_answers`")
                    db.execSQL("ALTER TABLE `cached_answers_new` RENAME TO `cached_answers`")
                }
            }

        @Volatile
        private var cachedInstance: ExamDatabase? = null

        fun getInstance(context: Context): ExamDatabase =
            cachedInstance ?: synchronized(this) {
                val instance =
                    Room
                        .databaseBuilder(
                            context.applicationContext,
                            ExamDatabase::class.java,
                            "exam",
                        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                        .build()
                cachedInstance = instance
                instance
            }
    }
}
