import org.gradle.api.artifacts.ProjectDependency

/*
 * Architecture guard rails (constitution C-ARCH / C-PAT / C-PRIV). `./gradlew verifyArchitecture` fails the build when:
 *  - a module depends on a module it must not know about (ui -> presentation -> domain <- data);
 *  - `:domain` imports anything related to Android, DI, networking or persistence;
 *  - `:presentation` touches Context/Activity/Application, Compose, CameraX, Room or HTTP types;
 *  - `:ui` reaches the data layer directly;
 *  - production code uses `runCatching` (it swallows cancellation), `GlobalScope` or `Dispatchers.*`
 *    (dispatchers are injected through `AppDispatchers`).
 */

val allowedProjectDependencies: Map<String, Set<String>> =
    mapOf(
        ":domain" to emptySet(),
        ":data" to setOf(":domain"),
        ":presentation" to setOf(":domain"),
        ":ui" to setOf(":presentation", ":domain"),
        ":app" to setOf(":ui", ":presentation", ":data", ":domain"),
        ":core:testing" to setOf(":domain"),
    )

class ImportRule(val modulePath: String, val sourceSets: List<String>, val forbidden: Regex, val reason: String)

val importRules: List<ImportRule> =
    listOf(
        ImportRule(
            modulePath = ":domain",
            sourceSets = listOf("main", "test"),
            forbidden =
            Regex(
                """^\s*import\s+(android\.|androidx\.|dagger\.|javax\.inject|jakarta\.inject|org\.koin|retrofit2|okhttp3|io\.ktor|com\.google\.|java\.io\.|java\.net\.|java\.nio\.)""",
            ),
            reason = ":domain must be pure Kotlin: no Android, DI, networking, persistence or platform I/O",
        ),
        ImportRule(
            modulePath = ":presentation",
            sourceSets = listOf("main"),
            forbidden =
            Regex(
                """^\s*import\s+(android\.content\.|android\.app\.|android\.view\.|android\.widget\.|android\.net\.|android\.graphics\.|androidx\.compose|androidx\.camera|androidx\.room|androidx\.activity|retrofit2|okhttp3|com\.tensoscan\.data)""",
            ),
            reason = ":presentation must not know Context/Activity/Application, Compose, CameraX, Room, HTTP or :data",
        ),
        ImportRule(
            modulePath = ":ui",
            sourceSets = listOf("main"),
            forbidden =
            Regex(
                """^\s*import\s+(retrofit2|okhttp3|androidx\.room|androidx\.datastore|com\.tensoscan\.data)""",
            ),
            reason = ":ui renders state; it must not reach the data layer",
        ),
    )

val forbiddenEverywhere: List<Pair<Regex, String>> =
    listOf(
        Regex("""\brunCatching\s*[({]""") to "use suspendRunCatching: runCatching swallows CancellationException",
        Regex("""\bGlobalScope\b""") to "GlobalScope breaks structured concurrency",
        Regex("""\bDispatchers\.(IO|Default|Main|Unconfined)\b""") to
            "inject AppDispatchers instead of using Dispatchers.* directly",
    )

val dispatcherAllowList = setOf("DispatcherModule.kt")

fun kotlinFiles(dir: File): List<File> =
    if (dir.exists()) dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList() else emptyList()

fun codeLines(file: File): List<Pair<Int, String>> = file.readLines().mapIndexedNotNull { index, line ->
    val trimmed = line.trim()
    val isComment = trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")
    if (isComment) null else (index + 1) to line
}

tasks.register("verifyArchitecture") {
    group = "verification"
    description = "Fails when module boundaries or layer purity rules are violated."

    doLast {
        val violations = mutableListOf<String>()

        subprojects.forEach { sub ->
            val allowed = allowedProjectDependencies[sub.path] ?: return@forEach
            sub.configurations
                .filter { it.name in setOf("api", "implementation", "compileOnly", "runtimeOnly") }
                .flatMap { configuration -> configuration.dependencies.withType(ProjectDependency::class.java) }
                .map { dependency -> dependency.path }
                .distinct()
                .filterNot { it in allowed }
                .forEach {
                    violations +=
                        "${sub.path} must not depend on $it (allowed: ${allowed.ifEmpty { setOf("nothing") }})"
                }
        }

        importRules.forEach { rule ->
            val sub = subprojects.firstOrNull { it.path == rule.modulePath } ?: return@forEach
            rule.sourceSets.forEach { sourceSet ->
                kotlinFiles(File(sub.projectDir, "src/$sourceSet")).forEach { file ->
                    codeLines(file)
                        .filter { (_, line) ->
                            rule.forbidden.containsMatchIn(line)
                        }.forEach { (number, line) ->
                            violations += "${file.relativeTo(rootDir)}:$number  ${line.trim()}  [${rule.reason}]"
                        }
                }
            }
        }

        subprojects.forEach { sub ->
            kotlinFiles(File(sub.projectDir, "src/main")).forEach { file ->
                codeLines(file).forEach { (number, line) ->
                    forbiddenEverywhere.forEach { (regex, reason) ->
                        val isDispatchers = reason.contains("AppDispatchers")
                        if (regex.containsMatchIn(line) && !(isDispatchers && file.name in dispatcherAllowList)) {
                            violations += "${file.relativeTo(rootDir)}:$number  ${line.trim()}  [$reason]"
                        }
                    }
                }
            }
        }

        if (violations.isNotEmpty()) {
            throw GradleException("Architecture violations:\n" + violations.joinToString("\n") { " - $it" })
        }
        println("Architecture OK")
    }
}
