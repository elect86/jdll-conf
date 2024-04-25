import io.bioimage.modelrunner.bioimageio.BioimageioRepo
import io.bioimage.modelrunner.bioimageio.description.SampleImage
import io.bioimage.modelrunner.bioimageio.description.TestArtifact
import io.bioimage.modelrunner.engine.EngineInfo
import io.bioimage.modelrunner.engine.installation.EngineInstall
import io.bioimage.modelrunner.model.Model
import io.bioimage.modelrunner.tensor.Tensor
import io.bioimage.modelrunner.versionmanagement.AvailableEngines
import io.bioimage.modelrunner.versionmanagement.InstalledEngines
import kotlinx.coroutines.*
import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.Channels
import kotlin.system.measureTimeMillis


val modelExt = project.extensions.create<ModelExt>("setModel").apply {
    dir.convention(model.map { projectDir / "models" / it.name })
}

val engineExt = project.extensions.create<EngineExt>("setEngine")

tasks {

    val generateModelsEnum by register("generateModelsEnum") {
        print("generating Models Enum.. ")
        val ms = measureTimeMillis {
            models.putAll(BioimageioRepo.connect().listAllModels(false))
        }
        println("took ${ms}ms")
        val accessorSrc = projectDir / "buildSrc/src/main/kotlin" / "Models.kt"
        if (!accessorSrc.exists())
            accessorSrc.createNewFile()
        val modelNames = models.entries
                .joinToString(separator = "\n") { (_, desc) ->
                    val name = desc.name.replace('.', ',')
                    """    `$name`("${desc.modelID}"),"""
                }
        val src = """
            |import io.bioimage.modelrunner.bioimageio.description.ModelDescriptor
            |
            |enum class Models(val id: String) {
            |$modelNames
            |}""".trimMargin()
        if (accessorSrc.readText() != src)
            accessorSrc.writeText(src)
        else println("Accessor file is identical, skipping")
    }

    val downloadModel by register("downloadModel") {
        dependsOn(generateModelsEnum)
        //        doLast {
        val dir = modelExt.dir.get()
        if (!dir.exists())
            dir.mkdirs()
        val model = models.values.first { it.modelID == modelExt.model.get().id }
        runBlocking {
            val jobs = ArrayList<Job>()
            fun download(url: URL) {
                jobs += launch(Dispatchers.IO) {
                    val name = url.path.substringBefore("/content").substringAfterLast('/')
                    val out = dir / name
                    if (!out.exists())
                        Channels.newChannel(url.openStream()).use { src ->
                            FileOutputStream(dir / name).use { dst ->
                                dst.channel.transferFrom(src, 0, Long.MAX_VALUE)
                            }
                        }
                    else {
                        val n = modelExt.model.get().name
                        println("$n/$name exists, skipping..")
                    }
                }
            }
            //            println(model)
            //        println(model.links)
            //        println(attachments)
            // addAttachments
            for (v in model.attachments.values) {
                if (v is String && v.isURL)
                    TODO("""downloadableLinks.put(" attachments_ " + c++, attachments.get(key) as String)""")
                else if (v is URL)
                    TODO("""downloadableLinks.put(" attachments_ " + c++, (attachments.get(key) as URL).toString())""")
                else if (v is Map<*, *>) {
                    TODO("map")
                    //                val nFilesMap = attachments.get(key) as Map<String, Any>
                    //                for (jj in nFilesMap.keys) {
                    //                    if (nFilesMap[jj] is String && DownloadModel.checkURL(nFilesMap[jj] as String?)) downloadableLinks.put(DownloadModel.ATTACH_KEY + "_" + c++, nFilesMap[jj].toString())
                    //                }
                } else if (v is List<*>)
                    for (e in v)
                        if (e is String && e.isURL)
                            download(URL(e))
            }
            // addRDF
            model.rdfSource!!.let {
                check(it.isURL)
                download(URL(it))
            }
            // addSampleInputs, addSampleOutputs
            val samples = buildList<SampleImage?> {
                model.sampleInputs?.let(::addAll)
                model.sampleOutputs?.let(::addAll)
            }
            for (sample in samples)
                sample?.url?.let(::download)

            // addTestInputs, addTestOutputs
            val artifacts = buildList<TestArtifact?> {
                model.testInputs?.let(::addAll)
                model.testOutputs?.let(::addAll)
            }
            for (artifact in artifacts)
                artifact?.url?.let(::download)

            // addWeights
            val weights = model.weights
            for (w in weights.gettAllSupportedWeightObjects())
                w.source?.let {
                    if (it.isURL)
                        download(URL(it))
                    //                    if (w.sourceFileName.endsWith(".zip")) unzip = true
                }
            //        }
            jobs.joinAll()
        }
    }

    val configureEngine by register("configureEngine") {
        dependsOn(downloadModel)
        val dir = engineExt.dir.getOrElse(projectDir / "engines")
        if (!dir.exists())
            dir.mkdirs()

        val engine = engineExt.engine.get()
        val cpu = engineExt.cpu.getOrElse(true)
        val possibleEngines = AvailableEngines.getEnginesForOsByParams(engine.framework.name,
                                                                       engine.version,
                                                                       cpu,
                                                                       null)
        val dl = possibleEngines.first()
        if ((dir / dl.folderName()).exists())
            println("engine already exists, skipping")
        else
            check(EngineInstall.installEngineInDir(dl, dir.absolutePath)) {
                "The wanted DL engine was not downloaed correctly: ${dl.folderName()}"
            }

        val installedList = InstalledEngines.checkEngineWithArgsInstalledForOS(engine.framework.name,
                                                                               engine.version,
                                                                               cpu, null, dir.absolutePath)
        val gpu = installedList.first().gpu
        val engineInfo = EngineInfo.defineDLEngine(engine.framework.name,
                                                   engine.version, cpu, gpu, dir.absolutePath)
        val modelFolder = modelExt.dir.get()
        val rdf = modelFolder / "rdf.yaml"
        val modelSource = rdf.readText()
                .lines().map { it.trim() }
                .dropWhile { it != "weights:" }
                .dropWhile { it != "${engine.framework.name}:" }
                .first { it.startsWith("source: ") }
                .drop("source: ".length).dropLast("/content".length)
                .substringAfterLast('/')

        val model = Model.createDeepLearningModel(modelFolder.absolutePath,
                                                  (modelFolder / modelSource).absolutePath,
                                                  engineInfo);
        val tensorInputs = ArrayList<Tensor<*>>()
        val tensorOutputs = ArrayList<Tensor<*>>()
        model.loadModel()
        execute(model, tensorInputs, tensorOutputs)
        model.closeModel()
        tensorInputs.forEach { it.close() }
        tensorOutputs.forEach { it.close() }
    }
}