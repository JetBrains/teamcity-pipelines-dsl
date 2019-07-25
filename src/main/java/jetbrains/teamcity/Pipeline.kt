import jetbrains.buildServer.configs.kotlin.v2018_2.ArtifactDependency
import jetbrains.buildServer.configs.kotlin.v2018_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2018_2.Project
import jetbrains.buildServer.configs.kotlin.v2018_2.SnapshotDependency
import java.lang.IllegalStateException

typealias DependencySettings = SnapshotDependency.() -> Unit

abstract class Stage {
    var dependencySettings: DependencySettings = {}
    val dependencies = mutableListOf<Pair<Stage, DependencySettings>>()
}

class Single(val buildType: BuildType) : Stage()

class Parallel : Stage() {
    val buildTypes = arrayListOf<BuildType>()
    val sequences = arrayListOf<Sequence>()
}

class Sequence : Stage() {
    val stages = arrayListOf<Stage>()
}

fun Parallel.build(bt: BuildType, block: BuildType.() -> Unit = {}): BuildType {
    bt.apply(block)
    buildTypes.add(bt)
    return bt
}

fun Parallel.build(block: BuildType.() -> Unit): BuildType {
    val bt = BuildType().apply(block)
    buildTypes.add(bt)
    return bt
}

fun Parallel.sequence(block: Sequence.() -> Unit): Sequence {
    val sequence = Sequence().apply(block)
    buildDependencies(sequence)
    sequences.add(sequence)
    return sequence
}

fun Sequence.sequence(block: Sequence.() -> Unit): Sequence {
    val sequence = Sequence().apply(block)
    buildDependencies(sequence)
    stages.add(sequence)
    return sequence
}

fun Sequence.parallel(block: Parallel.() -> Unit): Parallel {
    val parallel = Parallel().apply(block)
    stages.add(parallel)
    return parallel
}

fun Sequence.build(bt: BuildType, block: BuildType.() -> Unit = {}): BuildType {
    bt.apply(block)
    stages.add(Single(bt))
    return bt
}

fun Sequence.build(block: BuildType.() -> Unit): BuildType {
    val bt = BuildType().apply(block)
    stages.add(Single(bt))
    return bt
}

fun Project.sequence(block: Sequence.() -> Unit): Sequence {
    val sequence = Sequence().apply(block)
    buildDependencies(sequence)
    registerBuilds(sequence)
    return sequence
}

fun buildDependencies(sequence: Sequence) {
    var previous = sequence.dependencies.firstOrNull()

    for (stage in sequence.stages) {
        if (previous != null) {
            stageDependsOnStage(stage, Pair(previous.first, stage.dependencySettings))
        }
        stage.dependencies.forEach { dependency ->
            stageDependsOnStage(stage, dependency)
        }

        val dependencySettings = previous?.second ?: {}
        previous = Pair(stage, dependencySettings)
    }
}

fun stageDependsOnStage(stage: Stage, dependency: Pair<Stage, DependencySettings>) {
    val s = dependency.first
    val d = dependency.second
    if (s is Single) {
        stageDependsOnSingle(stage, Pair(s, d))
    }
    if (s is Parallel) {
        stageDependsOnParallel(stage, Pair(s, d))
    }
    if (s is Sequence) {
        stageDependsOnSequence(stage, Pair(s, d))
    }
}

fun stageDependsOnSingle(stage: Stage, dependency: Pair<Single, DependencySettings>) {
    if (stage is Single) {
        singleDependsOnSingle(stage, dependency)
    }
    if (stage is Parallel) {
        parallelDependsOnSingle(stage, dependency)
    }
    if (stage is Sequence) {
        sequenceDependsOnSingle(stage, dependency)
    }
}

fun stageDependsOnParallel(stage: Stage, dependency: Pair<Parallel, DependencySettings>) {
    if (stage is Single) {
        singleDependsOnParallel(stage, dependency)
    }
    if (stage is Parallel) {
        parallelDependsOnParallel(stage, dependency)
    }
    if (stage is Sequence) {
        sequenceDependsOnParallel(stage, dependency)
    }
}

fun stageDependsOnSequence(stage: Stage, dependency: Pair<Sequence, DependencySettings>) {
    if (stage is Single) {
        singleDependsOnSequence(stage, dependency)
    }
    if (stage is Parallel) {
        parallelDependsOnSequence(stage, dependency)
    }
    if (stage is Sequence) {
        sequenceDependsOnSequence(stage, dependency)
    }
}

fun singleDependsOnSingle(stage: Single, dependency: Pair<Single, DependencySettings>) {
    stage.buildType.dependencies.dependency(dependency.first.buildType) {
        snapshot(dependency.second)
    }
}

fun singleDependsOnParallel(stage: Single, dependency: Pair<Parallel, DependencySettings>) {
    dependency.first.buildTypes.forEach { buildType ->
        val dep = Pair(Single(buildType), dependency.second)
        singleDependsOnSingle(stage, dep)
    }
    dependency.first.sequences.forEach { sequence ->
        val dep = Pair(sequence, dependency.second)
        singleDependsOnSequence(stage, dep)
    }
}

fun singleDependsOnSequence(stage: Single, dependency: Pair<Sequence, DependencySettings>) {
    dependency.first.stages.lastOrNull()?.let { lastStage ->
        if (lastStage is Single) {
            singleDependsOnSingle(stage, Pair(lastStage, dependency.second))
        }
        if (lastStage is Parallel) {
            singleDependsOnParallel(stage, Pair(lastStage, dependency.second))
        }
        if (lastStage is Sequence) {
            singleDependsOnSequence(stage, Pair(lastStage, dependency.second))
        }
    }
}

fun parallelDependsOnSingle(stage: Parallel, dependency: Pair<Single, DependencySettings>) {
    stage.buildTypes.forEach { buildType ->
        val single = Single(buildType)
        singleDependsOnSingle(single, dependency)
    }
    stage.sequences.forEach { sequence ->
        sequenceDependsOnSingle(sequence, dependency)
    }
}

fun parallelDependsOnParallel(stage: Parallel, dependency: Pair<Parallel, DependencySettings>) {
    stage.buildTypes.forEach { buildType ->
        singleDependsOnParallel(Single(buildType), dependency)
    }
    stage.sequences.forEach { sequence ->
        sequenceDependsOnParallel(sequence, dependency)
    }
}

fun parallelDependsOnSequence(stage: Parallel, dependency: Pair<Sequence, DependencySettings>) {
    stage.buildTypes.forEach { buildType ->
        singleDependsOnSequence(Single(buildType), dependency)
    }
    stage.sequences.forEach { sequence ->
        sequenceDependsOnSequence(sequence, dependency)
    }
}

fun sequenceDependsOnSingle(stage: Sequence, dependency: Pair<Single, DependencySettings>) {
    stage.stages.firstOrNull()?.let { firstStage ->
        if (firstStage is Single) {
            singleDependsOnSingle(firstStage, dependency)
        }
        if (firstStage is Parallel) {
            parallelDependsOnSingle(firstStage, dependency)
        }
        if (firstStage is Sequence) {
            sequenceDependsOnSingle(firstStage, dependency)
        }
    }
}

fun sequenceDependsOnParallel(stage: Sequence, dependency: Pair<Parallel, DependencySettings>) {
    stage.stages.firstOrNull()?.let { firstStage ->
        if (firstStage is Single) {
            singleDependsOnParallel(firstStage, dependency)
        }
        if (firstStage is Parallel) {
            parallelDependsOnParallel(firstStage, dependency)
        }
        if (firstStage is Sequence) {
            sequenceDependsOnParallel(firstStage, dependency)
        }
    }
}

fun sequenceDependsOnSequence(stage: Sequence, dependency: Pair<Sequence, DependencySettings>) {
    stage.stages.firstOrNull()?.let { firstStage ->
        if (firstStage is Single) {
            singleDependsOnSequence(firstStage, dependency)
        }
        if (firstStage is Parallel) {
            parallelDependsOnSequence(firstStage, dependency)
        }
        if (firstStage is Sequence) {
            sequenceDependsOnSequence(firstStage, dependency)
        }
    }
}

fun Project.registerBuilds(sequence: Sequence) {
    sequence.stages.forEach {
        if (it is Single) {
            buildType(it.buildType)
        }
        if (it is Parallel) {
            it.buildTypes.forEach { build ->
                buildType(build)
            }
            it.sequences.forEach { seq ->
                registerBuilds(seq)
            }
        }
        if(it is Sequence) {
            registerBuilds(it)
        }
    }
}

fun Project.build(bt: BuildType, block: BuildType.() -> Unit = {}): BuildType {
    bt.apply(block)
    buildType(bt)
    return bt
}

fun Project.build(block: BuildType.() -> Unit): BuildType {
    val bt = BuildType().apply(block)
    buildType(bt)
    return bt
}

fun BuildType.produces(artifacts: String) {
    artifactRules = artifacts
}

fun BuildType.requires(bt: BuildType, artifacts: String, settings: ArtifactDependency.() -> Unit = {}) {
    dependencies.artifacts(bt) {
        artifactRules = artifacts
        settings()
    }
}

fun BuildType.dependsOn(bt: BuildType, settings: SnapshotDependency.() -> Unit = {}) {
    dependencies.dependency(bt) {
        snapshot(settings)
    }
}

/**
 * !!!WARNING!!!
 *
 * This method works as expected only if the <code>stage</code> is already populated
 */
fun BuildType.dependsOn(stage: Stage, dependencySettings: DependencySettings = {}) {
    val single = Single(this)
    single.dependsOn(stage, dependencySettings) //TODO: does it really work?
}

fun Stage.dependsOn(bt: BuildType, dependencySettings: DependencySettings = {}) {
    val stage = Single(bt)
    dependsOn(stage, dependencySettings)
}

fun Stage.dependsOn(stage: Stage, dependencySettings: DependencySettings = {}) {
    dependencies.add(Pair(stage, dependencySettings))
}

fun Stage.dependencySettings(dependencySettings: DependencySettings = {}) {
    this.dependencySettings = dependencySettings
}

fun BuildType.dependencySettings(dependencySettings: DependencySettings = {}) {
    throw IllegalStateException("dependencySettings can only be used with parallel {} or sequence {}. Please use dependsOn instead")
}


