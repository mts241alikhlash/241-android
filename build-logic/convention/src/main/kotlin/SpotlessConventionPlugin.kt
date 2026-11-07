import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class SpotlessConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("com.diffplug.spotless")
        target.extensions.configure<SpotlessExtension> {
            kotlin {
                target("**/*.kt")
                targetExclude("**/build/**")
                ktlint()
            }
            kotlinGradle {
                target("**/*.kts")
                targetExclude("**/build/**")
                ktlint()
            }
        }
    }
}
