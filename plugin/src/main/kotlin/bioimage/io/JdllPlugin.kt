package bioimage.io

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.provider.Property

interface DownloadModel {
//    val model: Property<Models>
}

class JdllPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        // Register a task
        project.tasks.register("greeting") { task ->
            task.doLast {
                println("Hello from plugin 'org.example.greeting'")
            }
        }
    }
}
