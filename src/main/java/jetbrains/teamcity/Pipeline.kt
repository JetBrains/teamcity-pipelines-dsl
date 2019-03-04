import jetbrains.buildServer.configs.kotlin.v2018_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2018_2.Project

interface Stage

class Single(val buildType: BuildType) : Stage

class Parallel : Stage {
    val buildTypes = arrayListOf<BuildType>()
    val sequences = arrayListOf<Sequence>()
}

class Sequence : Stage {
    val stages = arrayListOf<Stage>()
}

fun Parallel.build(bt: BuildType, block: BuildType.() -> Unit): BuildType {
    bt.apply(block)
    buildTypes.add(bt)
    return bt
}

fun Parallel.build(block: BuildType.() -> Unit): BuildType {
    val bt = BuildType().apply(block)
    buildTypes.add(bt)
    return bt
}

fun Parallel.build(bt: BuildType): BuildType {
    buildTypes.add(bt)
    return bt
}

fun Parallel.sequence(block: Sequence.() -> Unit): Sequence {
    val sequence = Sequence().apply(block)
    buildDependencies(sequence)
    sequences.add(sequence)
    return sequence
}

fun Sequence.sequence(block: Sequence.() -> Unit): Stage {
    val sequence = Sequence().apply(block)
    buildDependencies(sequence)
    stages.add(sequence)
    return sequence
}

fun Sequence.parallel(block: Parallel.() -> Unit): Stage {
    val parallel = Parallel().apply(block)
    stages.add(parallel)
    return parallel
}

fun Sequence.build(bt: BuildType, block: BuildType.() -> Unit): BuildType {
    bt.apply(block)
    stages.add(Single(bt))
    return bt
}

fun Sequence.build(block: BuildType.() -> Unit): BuildType {
    val bt = BuildType().apply(block)
    stages.add(Single(bt))
    return bt
}

fun Sequence.build(bt: BuildType): BuildType {
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
    var previous: Stage? = null

    for (stage in sequence.stages) {
        if (previous != null) {
            if (previous is Single) {
                stageDependsOnSingle(stage, previous)
            }
            if (previous is Parallel) {
                stageDependsOnParallel(stage, previous)
            }
            if (previous is Sequence) {
                stageDepdendsOnSequence(stage, previous)
            }
        }
        previous = stage
    }
}

fun stageDependsOnSingle(stage: Stage, dependency: Single) {
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

fun stageDependsOnParallel(stage: Stage, dependency: Parallel) {
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

fun stageDepdendsOnSequence(stage: Stage, dependency: Sequence) {
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

fun singleDependsOnSingle(stage: Single, dependency: Single) {
    stage.buildType.dependencies.snapshot(dependency.buildType) {}
}

fun singleDependsOnParallel(stage: Single, dependency: Parallel) {
    dependency.buildTypes.forEach { buildType ->
        singleDependsOnSingle(stage, Single(buildType))
    }
    dependency.sequences.forEach { sequence ->
        singleDependsOnSequence(stage, sequence)
    }
}

fun singleDependsOnSequence(stage: Single, dependency: Sequence) {
    dependency.stages.lastOrNull()?.let {lastStage ->
        if (lastStage is Single) {
            singleDependsOnSingle(stage, lastStage)
        }
        if (lastStage is Parallel) {
            singleDependsOnParallel(stage, lastStage)
        }
        if (lastStage is Sequence) {
            singleDependsOnSequence(stage, lastStage)
        }
    }
}

fun parallelDependsOnSingle(stage: Parallel, dependency: Single) {
    stage.buildTypes.forEach {buildType ->
        singleDependsOnSingle(Single(buildType), dependency)
    }
    stage.sequences.forEach {sequence ->
        sequenceDependsOnSingle(sequence, dependency)
    }
}

fun parallelDependsOnParallel(stage: Parallel, dependency: Parallel) {
    stage.buildTypes.forEach {buildType ->
        singleDependsOnParallel(Single(buildType), dependency)
    }
    stage.sequences.forEach {sequence ->
        sequenceDependsOnParallel(sequence, dependency)
    }
}

fun parallelDependsOnSequence(stage: Parallel, dependency: Sequence) {
    stage.buildTypes.forEach {buildType ->
        singleDependsOnSequence(Single(buildType), dependency)
    }
    stage.sequences.forEach {sequence ->
        sequenceDependsOnSequence(sequence, dependency)
    }
}

fun sequenceDependsOnSingle(stage: Sequence, dependency: Single) {
    stage.stages.firstOrNull()?.let {firstStage ->
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

fun sequenceDependsOnParallel(stage: Sequence, dependency: Parallel) {
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

fun sequenceDependsOnSequence(stage: Sequence, dependency: Sequence) {
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

private fun Project.registerBuilds(sequence: Sequence) {
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
    }
}

fun Project.build(bt: BuildType, block: BuildType.() -> Unit): BuildType {
    bt.apply(block)
    buildType(bt)
    return bt
}

fun Project.build(block: BuildType.() -> Unit): BuildType {
    val bt = BuildType().apply(block)
    buildType(bt)
    return bt
}

fun Project.build(bt: BuildType): BuildType {
    buildType(bt)
    return bt
}


fun BuildType.produces(artifacts: String) {
    artifactRules = artifacts
}

fun BuildType.requires(bt: BuildType, artifacts: String) {
    dependencies.artifacts(bt) {
        artifactRules = artifacts
    }
}