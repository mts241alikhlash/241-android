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
                            "number" to "kotlin.Double",
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
                        target.delete(
                            outDir.map { it.file("src/main/kotlin/${basePkg.replace('.', '/')}/infrastructure/ApiClient.kt") },
                            target.fileTree(outDir.map { it.dir("src/main/kotlin/${basePkg.replace('.', '/')}/models") }) {
                                include("HealthControllerCheck*.kt")
                            },
                        )
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
        target.tasks.matching { it.name.startsWith("ksp") || it.name.startsWith("spotless") }.configureEach {
            dependsOn(generateTasks)
        }
    }
}
