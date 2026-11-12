import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

private data class OpenApiServiceSpec(
    val service: String,
    val taskSuffix: String,
    val operationFilter: String,
)

class OpenApiClientConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("org.openapi.generator")

        val specs =
            listOf(
                OpenApiServiceSpec(
                    service = "exam",
                    taskSuffix = "Exam",
                    operationFilter =
                        listOf(
                            "StudentExamController_findMine",
                            "StudentExamController_start",
                            "SessionExamController_questions",
                            "SessionExamController_answers",
                            "SessionExamController_save",
                            "SessionExamController_saveBatch",
                            "SessionExamController_violations",
                            "SessionExamController_events",
                            "SessionExamController_heartbeatNow",
                            "SessionExamController_suspension",
                            "SessionExamController_resume",
                            "SessionExamController_cameraBlocked",
                            "SessionExamController_snapshot",
                            "SessionExamController_submit",
                            "SessionExamController_file",
                        ).joinToString("|", prefix = "operationId:"),
                ),
                OpenApiServiceSpec(
                    service = "identity",
                    taskSuffix = "Identity",
                    operationFilter =
                        listOf(
                            "NativeAuthController_login",
                            "NativeAuthController_refresh",
                            "NativeAuthController_logout",
                        ).joinToString("|", prefix = "operationId:"),
                ),
                OpenApiServiceSpec(
                    service = "student",
                    taskSuffix = "Student",
                    operationFilter =
                        listOf(
                            "StudentController_findMine",
                            "StudentController_findMyClassroom",
                            "StudentController_findMyParticipantNumber",
                        ).joinToString("|", prefix = "operationId:"),
                ),
            )

        val generateTasks =
            specs.map { spec ->
                val outDir = target.layout.buildDirectory.dir("generated/openapi/${spec.service}")
                target.tasks.register<GenerateTask>("generate${spec.taskSuffix}OpenApi") {
                    generatorName.set("kotlin")
                    library.set("jvm-retrofit2")
                    inputSpec.set("${target.projectDir}/contracts/${spec.service}.openapi.json")
                    outputDir.set(outDir)
                    val basePkg = "id.sch.mts241alikhlash.exam.api.${spec.service}"
                    packageName.set(basePkg)
                    apiPackage.set("$basePkg.apis")
                    modelPackage.set("$basePkg.models")
                    generateApiTests.set(false)
                    generateModelTests.set(false)
                    generateApiDocumentation.set(false)
                    generateModelDocumentation.set(false)
                    openapiNormalizer.set(
                        mapOf(
                            "FILTER" to spec.operationFilter,
                        ),
                    )
                    typeMappings.set(
                        mapOf(
                            "UUID" to "kotlin.String",
                            "DateTime" to "kotlin.String",
                            "Date" to "kotlin.String",
                            "number" to "kotlin.Int",
                            "decimal" to "kotlin.Double",
                            "AnyType" to "JsonElement",
                            "object" to "JsonElement",
                        ),
                    )
                    importMappings.set(
                        mapOf(
                            "JsonElement" to "kotlinx.serialization.json.JsonElement",
                        ),
                    )
                    configOptions.set(
                        mapOf(
                            "library" to "jvm-retrofit2",
                            "serializationLibrary" to "kotlinx_serialization",
                            "useCoroutines" to "true",
                            "omitGradleWrapper" to "true",
                        ),
                    )
                    doFirst {
                        target.delete(outDir)
                    }
                    doLast {
                        val pkgPath = basePkg.replace('.', '/')
                        target.delete(
                            outDir.map { it.file("src/main/kotlin/$pkgPath/infrastructure/ApiClient.kt") },
                            target.fileTree(outDir.map { it.dir("src/main/kotlin/$pkgPath/models") }) {
                                include("HealthControllerCheck*.kt")
                            },
                        )
                        val apiDir = outDir.get().dir("src/main/kotlin/$pkgPath/apis").asFile
                        apiDir.walkTopDown().filter { it.isFile && it.name == "StudentExamsApi.kt" }.forEach { file ->
                            var text = file.readText()
                            text =
                                text
                                    .replace("@Part(\"kind\") kind: kotlin.String", "@Part(\"kind\") kind: RequestBody")
                                    .replace("@Part(\"capturedAt\") capturedAt: kotlin.String", "@Part(\"capturedAt\") capturedAt: RequestBody")
                            file.writeText(text)
                        }
                        val modelsDir = outDir.get().dir("src/main/kotlin/$pkgPath/models").asFile
                        modelsDir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
                            var text = file.readText()
                            text =
                                text.replace(
                                    "@Contextual @SerialName(value = \"answer\")",
                                    "@SerialName(value = \"answer\")",
                                )
                            text = Regex("kotlin\\.Int\\(\"(\\d+)\"\\)").replace(text, "$1")
                            when (file.name) {
                                "PaperQuestionDto.kt" -> {
                                    text = text.replace("val points: kotlin.Int", "val points: kotlin.Double")
                                }
                                "MyExamDto.kt" -> {
                                    text =
                                        text
                                            .replace("val subjectName: kotlin.String?,", "val subjectName: kotlin.String? = null,")
                                            .replace("val minSubmitMinutes: kotlin.Int,", "val minSubmitMinutes: kotlin.Int = 0,")
                                            .replace("val sessionId: kotlin.String?,", "val sessionId: kotlin.String? = null,")
                                            .replace("val activityId: kotlin.String?,", "val activityId: kotlin.String? = null,")
                                            .replace("val activityStatus: MyExamDto.ActivityStatus?,", "val activityStatus: MyExamDto.ActivityStatus? = null,")
                                            .replace("val eventId: kotlin.String?,", "val eventId: kotlin.String? = null,")
                                            .replace("val eventName: kotlin.String?,", "val eventName: kotlin.String? = null,")
                                            .replace("val roomName: kotlin.String?,", "val roomName: kotlin.String? = null,")
                                            .replace("val seatNumber: kotlin.Int?,", "val seatNumber: kotlin.Int? = null,")
                                            .replace("val participantNumber: kotlin.String?,", "val participantNumber: kotlin.String? = null,")
                                            .replace("val makeup: kotlin.Boolean\n", "val makeup: kotlin.Boolean = false\n")
                                }
                                "StartedSessionDto.kt" -> {
                                    text =
                                        text
                                            .replace("val deadline: kotlin.Int", "val deadline: kotlin.Long")
                                            .replace("val serverTime: kotlin.Int", "val serverTime: kotlin.Long")
                                            .replace("val policy: ExamPolicyDto", "val policy: ExamPolicyDto = ExamPolicyDto()")
                                }
                                "ExamPolicyDto.kt" -> {
                                    text =
                                        text
                                            .replace("val violationLimit: kotlin.Int", "val violationLimit: kotlin.Int = 3")
                                            .replace("val focusThresholdMs: kotlin.Int", "val focusThresholdMs: kotlin.Long = 10_000L")
                                            .replace("val snapshotIntervalMs: kotlin.Int", "val snapshotIntervalMs: kotlin.Long = 15_000L")
                                            .replace("val heartbeatIntervalMs: kotlin.Int", "val heartbeatIntervalMs: kotlin.Long = 15_000L")
                                            .replace("val gapRecordedMs: kotlin.Int", "val gapRecordedMs: kotlin.Long = 60_000L")
                                            .replace("val gapTokenMs: kotlin.Int", "val gapTokenMs: kotlin.Long = 300_000L")
                                            .replace("val gapViolationFrom: kotlin.Int", "val gapViolationFrom: kotlin.Int = 2")
                                            .replace("val tokenSlotSeconds: kotlin.Int", "val tokenSlotSeconds: kotlin.Int = 600")
                                }
                                "NativeAuthResponseDto.kt" -> {
                                    text = text.replace("val expiresIn: kotlin.Int", "val expiresIn: kotlin.Long")
                                }
                            }
                            file.writeText(text)
                        }
                        val infraDir = outDir.get().dir("src/main/kotlin/$pkgPath/infrastructure").asFile
                        if (infraDir.exists()) {
                            infraDir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
                                val text = file.readText()
                                if (!text.startsWith("@file:Suppress(\"NewApi\")")) {
                                    file.writeText("@file:Suppress(\"NewApi\")\n\n$text")
                                }
                            }
                        }
                    }
                }
            }

        target.extensions.configure<ApplicationExtension> {
            sourceSets.getByName("main") {
                specs.forEach { spec ->
                    kotlin.srcDir(
                        target.layout.buildDirectory.dir("generated/openapi/${spec.service}/src/main/kotlin"),
                    )
                }
            }
        }

        target.tasks.withType(KotlinCompile::class.java).configureEach {
            dependsOn(generateTasks)
        }
        target.tasks.matching { it.name.startsWith("ksp") || it.name.startsWith("spotless") || it.name.startsWith("lint") }.configureEach {
            dependsOn(generateTasks)
        }
    }
}
