import jetbrains.buildServer.configs.kotlin.v2018_2.*
import net.sourceforge.plantuml.SourceStringReader
import java.io.File
import java.io.FileOutputStream


/*
   sequence    := (stage)+
   stage    := (serial|parallel)+
   parallel := (serial|sequence)+
   serial   := build
 */

version = "2018.2"

project {
    val settings: SnapshotDependency.() -> Unit = {
        runOnSameAgent = true
        onDependencyCancel = FailureAction.IGNORE
        reuseBuilds = ReuseBuilds.NO
    }

    sequence {
        val other = build {
            id("other")
        }
        build(Compile) {
            produces("application.jar")
        }
        val parallel = parallel {
            dependsOn(other, settings)
            build { id("aaa") }
            build { id("bbb") }
        }
        build { id("inTheMiddle") }
        parallel {
            dependsOn(other, settings)
            build(Test1)
            sequence {
                build(Test2)
                build(Test3)
            }

            dependencySettings {
                runOnSameAgent = true
                onDependencyCancel = FailureAction.FAIL_TO_START
                reuseBuilds = ReuseBuilds.ANY
            }
        }
        build(Package) {
            requires(Compile, "application.jar")
            produces("application.zip")
        }
        build {
            id("ccc")
            dependsOn(parallel, settings)
        }
        build(Publish) {
            requires(Package, "application.zip")
        }
    }

    //region plantUml
    val out = FileOutputStream(File("image.png"))
    val reader = SourceStringReader(plantUml())
    reader.generateImage(out)
    //endregion

    println()
}

object Compile : BuildType({
    name = "Compile"

})

object Test : BuildType({
    name = "Test"
})

object Test1 : BuildType({
    name = "Test1"
})

object Test2 : BuildType({
    name = "Test2"
})

object Test3 : BuildType({
    name = "Test3"
})

object Test4 : BuildType({
    name = "Test4"
})

object Package : BuildType({
    name = "Package"
})

object Publish : BuildType({
    name = "Publish"
})

object Deploy : BuildType({
    name = "Deploy"
})


fun Project.plantUml() : String {



    var plantUml = "@startuml\n\n"

    plantUml += "(*) --> \"${buildTypes.first().id?.value}\"\n"

    buildTypes.forEach { buildType ->
        buildType.dependencies.items.forEach { dependency ->
            dependency.snapshot?.let {
                //runOnSameAgent=${it.runOnSameAgent}
                //onDependencyCancel=${it.onDependencyCancel}
                //runDependencyFailure=${it.onDependencyFailure}
                //reuseBuilds=${it.reuseBuilds}
                plantUml += "\"${dependency.buildTypeId.id?.value}\" --> [sameAgent=${it.runOnSameAgent} dependencyCancel=${it.onDependencyCancel} reuseBuilds=${it.reuseBuilds}] \"${buildType.id}\"\n"
            }
            dependency.artifacts.forEach { artifact ->
                plantUml += "\"${dependency.buildTypeId.id?.value}\" --> [A: ${artifact.artifactRules}] \"${buildType.id}\"\n"
            }
        }
    }

    plantUml += "\"${buildTypes.last().id?.value}\" --> (*)\n"

    plantUml += "\n@enduml"

    return plantUml
}
