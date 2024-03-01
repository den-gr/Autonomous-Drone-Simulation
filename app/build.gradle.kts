
plugins {
//    alias(libs.plugins.kotlin.qa)
    alias(libs.plugins.multiJvmTesting)
    alias(libs.plugins.taskTree)
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.bundles.alchemist)
    implementation(libs.slf4j)
    implementation(fileTree("../libs"))
    testImplementation(kotlin("test"))
    implementation("com.github.haifengl:smile-core:3.0.2")
    implementation("com.github.haifengl:smile-kotlin:3.0.2")
}

multiJvm {
    jvmVersionForCompilation.set(latestJava)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

sourceSets {
    main {
        resources {
            srcDir("src/main/protelis")
        }
    }
}

val alchemistGroup = "Run Alchemist"
val alchemistBatchGroup = "Run Alchemist in Batch"
/*
 * This task is used to run all experiments in sequence
 */
val runAll by tasks.register<DefaultTask>("runAll") {
    group = alchemistGroup
    description = "Launches all simulations"
}

val batch: String by project
val maxTime: String by project

/*
 * Scan the folder with the simulation files, and create a task for each one of them.
 */
File(rootProject.rootDir.path + "/app/src/main/yaml").listFiles()
    ?.filter { it.extension == "yml" } // pick all yml files in src/main/yaml
    ?.sortedBy { it.nameWithoutExtension } // sort them, we like reproducibility
    ?.forEach {
        // one simulation file -> one gradle task
        val task by tasks.register<JavaExec>("run${it.nameWithoutExtension.uppercase()}") {
            group = alchemistGroup // This is for better organization when running ./gradlew tasks
            description = "Launches simulation ${it.nameWithoutExtension}" // Just documentation
            mainClass.set("it.unibo.alchemist.Alchemist") // The class to launch
            classpath = sourceSets["main"].runtimeClasspath // The classpath to use
            // Uses the latest version of java
            javaLauncher.set(
                javaToolchains.launcherFor {
                    languageVersion.set(JavaLanguageVersion.of(multiJvm.latestJava))
                },
            )
            // These are the program arguments
            args("run", it.absolutePath, "--override")

            if (System.getenv("CI") == "true" || batch == "true") {
                // If it is running in a Continuous Integration environment, use the "headless" mode of the simulator
                // Namely, force the simulator not to use graphical output.
                args(
                    """
                        terminate:
                        - type: AfterTime
                          parameters: $maxTime
                    """.trimIndent(),
                )
            } else {
                // A graphics environment should be available, so load the effects for the UI from the "effects" folder
                // Effects are expected to be named after the simulation file
                args(
                    """
                        launcher:
                          type: SingleRunSwingUI
                          parameters:
                            graphics: ../effects/${it.nameWithoutExtension}.json
                    """,
                )
            }
        }
        tasks.register<JavaExec>("runBatch${it.nameWithoutExtension.uppercase()}") {
            group = alchemistBatchGroup // This is for better organization when running ./gradlew tasks
            description = "Launches simulation ${it.nameWithoutExtension}" // Just documentation
            mainClass.set("it.unibo.alchemist.Alchemist") // The class to launch
            classpath = sourceSets["main"].runtimeClasspath // The classpath to use
            // Uses the latest version of java
            javaLauncher.set(
                javaToolchains.launcherFor {
                    languageVersion.set(JavaLanguageVersion.of(multiJvm.latestJava))
                },
            )
            // These are the program arguments
            args("run", it.absolutePath, "--override")
            args(
                """
                    launcher:
                        type: HeadlessSimulationLauncher
                        parameters: [["Seed", "ClusteringDistance", "CamHerdRatio", "NumberOfHerds", "Algorithm"]]
                    terminate:
                      - type: AfterTime
                        parameters: 1800
                """,
            )
        }
        runAll.dependsOn(task)
    }

tasks.withType<Tar>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.WARN
}
tasks.withType<Zip>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.WARN
}
