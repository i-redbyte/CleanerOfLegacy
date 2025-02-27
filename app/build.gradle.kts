import com.android.build.gradle.BaseExtension

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "su.redbyte.cleaneroflegacy"
    compileSdk = 34

    defaultConfig {
        applicationId = "su.redbyte.cleaneroflegacy"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

fun getCurrentVersion(): String {
    val android = project.extensions.findByType(BaseExtension::class.java)
        ?: error("Android extension not found")

    return android.defaultConfig.versionName
        ?: error("Unable to find versionName in DefaultConfig")
}

tasks.register("removeDeprecatedCode", Delete::class) {
    outputs.upToDateWhen { false }

    doLast {
        val currentVersion = getCurrentVersion()
        println("Current app version: $currentVersion")

        allprojects.forEach { project ->
            val sourceDir = project.file("src/main/java")
            if (sourceDir.exists()) {
                println("Processing module: ${project.name}")
                processSourceFiles(sourceDir, currentVersion)
            }
        }
    }
}

fun processSourceFiles(sourceDir: File, currentVersion: String) {
    sourceDir.walk().filter { it.isFile && it.extension == "kt" }.forEach { file ->
        val lines = file.readLines()

        val isDeclarationAnnotated = isDeclarationAnnotatedForRemoval(lines, currentVersion)

        if (isDeclarationAnnotated) {
            println("Deleting file: ${file.path} (declaration is annotated with @RemoveAfter)")
            file.delete()
            return@forEach
        }

        val modifiedLines = mutableListOf<String>()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]

            if (line.contains("@RemoveAfter")) {
                val match = Regex("""@RemoveAfter\("(\d+\.\d+\.\d+)"\)""").find(line)
                if (match != null) {
                    val targetVersion = match.groupValues[1]
                    if (isVersionGreaterOrEqual(currentVersion, targetVersion)) {
                        println("Marking function for removal in file: ${file.path}, target version: $targetVersion")
                        i = skipFunction(lines, i)
                        continue
                    }
                }
            }

            modifiedLines.add(line)
            i++
        }

        if (modifiedLines.size < lines.size) {
            println("Updating file: ${file.path}")
            file.writeText(modifiedLines.joinToString("\n"))
        } else {
            println("No changes needed for file: ${file.path}")
        }
    }
}

fun isDeclarationAnnotatedForRemoval(lines: List<String>, currentVersion: String): Boolean {
    var i = 0
    while (i < lines.size) {
        val line = lines[i]

        if (line.contains("@RemoveAfter")) {
            val match = Regex("""@RemoveAfter\("(\d+\.\d+\.\d+)"\)""").find(line)
            if (match != null) {
                val targetVersion = match.groupValues[1]
                if (isVersionGreaterOrEqual(currentVersion, targetVersion)) {
                    if (i + 1 < lines.size && isKotlinDeclaration(lines[i + 1].trim())) {
                        return true
                    }
                }
            }
        }

        i++
    }
    return false
}

fun isKotlinDeclaration(line: String): Boolean {
    return line.startsWith("class ") ||
            line.startsWith("interface ") ||
            line.startsWith("object ") ||
            line.startsWith("enum class ") ||
            line.startsWith("data class ") ||
            line.startsWith("sealed class ") ||
            line.startsWith("abstract class ")
}

fun skipFunction(lines: List<String>, startIndex: Int): Int {
    var i = startIndex
    var braceCount = 0
    var isFunctionStarted = false

    while (i < lines.size) {
        val line = lines[i]

        if (line.contains("fun ")) {
            isFunctionStarted = true
        }

        if (isFunctionStarted) {
            braceCount += line.count { it == '{' }
            braceCount -= line.count { it == '}' }

            if (braceCount == 0) {
                i++
                if (i < lines.size && lines[i].isBlank()) { //todo: if or while?
                    i++
                }
                break
            }
        }

        i++
    }

    return i
}

fun isVersionGreaterOrEqual(current: String, target: String): Boolean {
    val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
    val targetParts = target.split(".").mapNotNull { it.toIntOrNull() }
    for ((c, t) in currentParts.zip(targetParts)) {
        if (c > t) return true
        if (c < t) return false
    }
    return currentParts.size >= targetParts.size
}

tasks.named("preBuild") {
    dependsOn("removeDeprecatedCode")
}