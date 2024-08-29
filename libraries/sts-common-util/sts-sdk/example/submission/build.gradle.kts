plugins { id("com.android.sts.sdksubmission") }

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

// NOTE! all STS SDK dependencies must be subprojects
dependencies {
    // Automatically add each subproject as an STS SDK Test Resource
    getSubprojects().forEach { stsSdkTestResource(it) }
}

stsSdkSubmission {
    // Please configure your submission attributes here
}
