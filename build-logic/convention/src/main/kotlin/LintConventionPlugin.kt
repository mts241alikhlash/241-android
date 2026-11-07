import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.Lint
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class LintConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.withPlugin("com.android.application") {
                extensions.configure<ApplicationExtension> { lint { configureLint() } }
            }
            pluginManager.withPlugin("com.android.library") {
                extensions.configure<LibraryExtension> { lint { configureLint() } }
            }
        }
}

private fun Lint.configureLint() {
    warningsAsErrors = true
    disable +=
        setOf(
            "LockedOrientationActivity",
            "NonResizeableActivity",
            "DiscouragedApi",
            "HardwareIds",
            "OldTargetApi",
            "GradleDependency",
            "AndroidGradlePluginVersion",
            "IconLauncherShape",
        )
    explainIssues = true
}
