import jetbrains.buildServer.configs.kotlin.v2018_2.*
//import net.sourceforge.plantuml.SourceStringReader
//import java.io.File
//import java.io.FileOutputStream


/*
   sequence    := (stage)+
   stage    := (serial|parallel)+
   parallel := (serial|sequence)+
   serial   := build
 */

version = "2018.2"

project {


    val build = build {
        id("other")
    }

    sequence {
        build(Compile)
        parallel {
            build(Test){
                dependsOn(build){

                }
                produces("artifact")
                requires(Compile, "file.txt")
            }
            sequence {
                build(Test1) {
                    dependsOn(build){
                        runOnSameAgent = true
                    }
                }
                build(Test2)
            }
        }
        build(Package)
    }

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


//region plantUml
//fun Project.plantUml() {
//    //region plantUml
//    val out = FileOutputStream(File("image.png"))
//    val reader = SourceStringReader(ascii())
//    reader.generateImage(out)
//    //endregion
//}
//
//fun Project.ascii(): String {
//
//    var plantUml = "@startuml\n\n"
//
//    plantUml += "(*) --> \"${buildTypes.first().id?.value}\"\n"
//
//    buildTypes.forEach { buildType ->
//        buildType.dependencies.items.forEach { dependency ->
//            dependency.snapshot?.let {
//                //runOnSameAgent=${it.runOnSameAgent}
//                //onDependencyCancel=${it.onDependencyCancel}
//                //runDependencyFailure=${it.onDependencyFailure}
//                //reuseBuilds=${it.reuseBuilds}
//                plantUml += "\"${dependency.buildTypeId.id?.value}\" --> [sameAgent=${it.runOnSameAgent} dependencyCancel=${it.onDependencyCancel} reuseBuilds=${it.reuseBuilds}] \"${buildType.id}\"\n"
//            }
//            dependency.artifacts.forEach { artifact ->
//                plantUml += "\"${dependency.buildTypeId.id?.value}\" --> [A: ${artifact.artifactRules}] \"${buildType.id}\"\n"
//            }
//        }
//    }
//
//    plantUml += "\"${buildTypes.last().id?.value}\" --> (*)\n"
//
//    plantUml += "\n@enduml"
//
//    return plantUml
//}
//endregion
