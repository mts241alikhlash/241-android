import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withGroovyBuilder

class RoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("com.google.devtools.ksp")
        target.extensions.getByName("ksp").withGroovyBuilder {
            "arg"("room.schemaLocation", "${target.projectDir}/schemas")
        }
    }
}
