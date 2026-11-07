package id.sch.mts241alikhlash.exam.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExamDatabaseMigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            ExamDatabase::class.java,
        )

    @Before
    fun requireSchemaAssets() {
        val assets =
            InstrumentationRegistry
                .getInstrumentation()
                .context
                .assets
        val present =
            runCatching {
                assets.open("$SCHEMA_DIR/1.json").close()
            }.isSuccess
        assumeTrue(
            "Room schema assets are only packaged for debug/androidTest variants",
            present,
        )
    }

    @Test
    fun `migration 1 to 2 creates cached_questions with composite primary key`() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO cached_answers " +
                    "(questionId, sessionId, answer, isDoubtful, isSynced, updatedAt) " +
                    "VALUES ('q1', 's1', 'A', 0, 0, 0)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, ExamDatabase.MIGRATION_1_2)

        db
            .query(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'cached_questions'",
            ).use { cursor ->
                assertTrue("cached_questions table should exist", cursor.moveToFirst())
            }

        val primaryKeyColumns = mutableListOf<Pair<String, Int>>()
        db.query("PRAGMA table_info(`cached_questions`)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            val pkIndex = cursor.getColumnIndexOrThrow("pk")
            while (cursor.moveToNext()) {
                val pk = cursor.getInt(pkIndex)
                if (pk > 0) primaryKeyColumns.add(cursor.getString(nameIndex) to pk)
            }
        }
        assertEquals(
            listOf("sessionId" to 1, "id" to 2),
            primaryKeyColumns.sortedBy { it.second },
        )

        db.query("SELECT COUNT(*) FROM cached_answers").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
        db.close()
    }

    @Test
    fun `migrated cached_questions accepts composite key rows`() {
        helper.createDatabase(TEST_DB, 1).close()
        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, ExamDatabase.MIGRATION_1_2)

        db.execSQL(
            "INSERT INTO cached_questions " +
                "(sessionId, id, number, type, bodyHtml, choicesJson, cachedAt) " +
                "VALUES ('s1', 'q1', 1, 'multiple_choice', '<p>x</p>', NULL, 0)",
        )
        db.execSQL(
            "INSERT INTO cached_questions " +
                "(sessionId, id, number, type, bodyHtml, choicesJson, cachedAt) " +
                "VALUES ('s2', 'q1', 1, 'multiple_choice', '<p>x</p>', NULL, 0)",
        )
        db.query("SELECT COUNT(*) FROM cached_questions").use { cursor ->
            cursor.moveToFirst()
            assertEquals(2, cursor.getInt(0))
        }
        db.close()
    }

    @Test
    fun `migration 2 to 3 keeps unsynced answers, clears the rest and adds the points column`() {
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL(
                "INSERT INTO cached_answers " +
                    "(questionId, sessionId, answer, isDoubtful, isSynced, updatedAt) " +
                    "VALUES ('q1', 's1', 'A', 0, 0, 0)",
            )
            execSQL(
                "INSERT INTO cached_answers " +
                    "(questionId, sessionId, answer, isDoubtful, isSynced, updatedAt) " +
                    "VALUES ('q2', 's1', 'B', 0, 1, 0)",
            )
            execSQL(
                "INSERT INTO cached_questions " +
                    "(sessionId, id, number, type, bodyHtml, choicesJson, cachedAt) " +
                    "VALUES ('s1', 'q1', 1, 'multiple_choice', '<p>x</p>', '[\"A\"]', 0)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true, ExamDatabase.MIGRATION_2_3)

        db.query("SELECT questionId FROM cached_answers").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("q1", cursor.getString(0))
            assertEquals(false, cursor.moveToNext())
        }
        db.query("SELECT COUNT(*) FROM cached_questions").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        db.execSQL(
            "INSERT INTO cached_questions " +
                "(sessionId, id, number, type, bodyHtml, points, choicesJson, cachedAt) " +
                "VALUES ('s1', 'q1', 1, 'MULTIPLE_CHOICE', '<p>x</p>', 2.5, NULL, 0)",
        )
        db.query("SELECT points FROM cached_questions").use { cursor ->
            cursor.moveToFirst()
            assertEquals(2.5, cursor.getDouble(0), 0.0)
        }
        db.close()
    }

    @Test
    fun `migration 3 to 4 adds an empty session_events table`() {
        helper.createDatabase(TEST_DB, 3).close()

        val db = helper.runMigrationsAndValidate(TEST_DB, 4, true, ExamDatabase.MIGRATION_3_4)

        db.execSQL(
            "INSERT INTO session_events " +
                "(clientId, sessionId, type, isViolation, occurredAtMs, detail, isSynced) " +
                "VALUES ('c1', 's1', 'LEFT_APP', 1, 1000, NULL, 0)",
        )
        db.query("SELECT COUNT(*) FROM session_events").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
        db.close()
    }

    @Test
    fun `migration 4 to 5 maps isSynced onto the sync state`() {
        helper.createDatabase(TEST_DB, 4).apply {
            execSQL(
                "INSERT INTO cached_answers " +
                    "(questionId, sessionId, answer, isDoubtful, isSynced, updatedAt) " +
                    "VALUES ('q1', 's1', 'A', 0, 0, 1000)",
            )
            execSQL(
                "INSERT INTO cached_answers " +
                    "(questionId, sessionId, answer, isDoubtful, isSynced, updatedAt) " +
                    "VALUES ('q2', 's1', 'B', 1, 1, 2000)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 5, true, ExamDatabase.MIGRATION_4_5)

        db
            .query(
                "SELECT questionId, syncState, updatedAtMillis, confirmedAtMillis, clientId " +
                    "FROM cached_answers ORDER BY questionId",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("q1", cursor.getString(0))
                assertEquals("LOCAL_PENDING", cursor.getString(1))
                assertEquals(1000L, cursor.getLong(2))
                assertTrue("an unsynced row has no confirmation stamp", cursor.isNull(3))
                assertTrue("every row must carry a client id", cursor.getString(4).isNotBlank())

                assertTrue(cursor.moveToNext())
                assertEquals("q2", cursor.getString(0))
                assertEquals("SERVER_CONFIRMED", cursor.getString(1))
                assertEquals(2000L, cursor.getLong(2))
                assertEquals(2000L, cursor.getLong(3))
                assertEquals(false, cursor.moveToNext())
            }
        db.close()
    }

    private companion object {
        const val TEST_DB = "migration-test"
        const val SCHEMA_DIR = "id.sch.mts241alikhlash.exam.data.local.ExamDatabase"
    }
}
