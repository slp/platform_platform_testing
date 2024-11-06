plugins { id("com.android.security.autorepro.submission") }

fun getSubprojects(): List<Project> {
    // glob for gradle direct subprojects
    return fileTree(projectDir) { include("*/build.gradle*") }
        .map {
            val path = projectDir.toPath().relativize(it.toPath().getParent())
            val gradlePath = path.toString().replace('/', ':')
            gradlePath
        }
        .filter({
            // filter out self build.gradle*
            !it.isEmpty()
        })
        .map { project(it) }
}

// NOTE! all AutoRepro dependencies must be subprojects
dependencies {
    // Automatically add each subproject as an AutoRepro Test Resource
    getSubprojects().forEach { testResource(it) }
}

submission {
    // Please configure your submission attributes here
}
