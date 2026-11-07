import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withGroovyBuilder

class RoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("com.google.devtools.ksp")
        // shortcut: KspExtension is not on this classpath (compiled with a newer Kotlin than kotlin-dsl embeds), upgrade when Gradle's embedded Kotlin catches up
        target.extensions.getByName("ksp").withGroovyBuilder {
            "arg"("room.schemaLocation", "${target.projectDir}/schemas")
        }
    }
}
