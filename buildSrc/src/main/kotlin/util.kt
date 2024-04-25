import io.bioimage.modelrunner.model.Model
import io.bioimage.modelrunner.tensor.Tensor
import net.imglib2.img.ImgFactory
import net.imglib2.img.array.ArrayImgFactory
import net.imglib2.type.NativeType
import net.imglib2.type.numeric.RealType
import net.imglib2.type.numeric.real.FloatType
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.getByName
import java.io.File
import java.net.MalformedURLException
import java.net.URL

sealed class Framework(val name: String) {
    object torchscript : Framework("torchscript") {
        val `1,13,1` = Engine(this, "1.13.1")
    }
}

data class Engine(val framework: Framework, val version: String)

operator fun File.div(path: String) = File(this, path)

internal val String.isURL: Boolean
    get() = try {
        URL(this)
        true
    } catch (e: MalformedURLException) {
        false
    }

interface ModelExt {
    val model: Property<Models>
    val dir: Property<File>
}

interface EngineExt {
    val engine: Property<Engine>
    val cpu: Property<Boolean>
    val gpu: Property<Boolean>
    val dir: Property<File>
}

lateinit var execute: (model: Model,
                       inputs: ArrayList<Tensor<*>>,
                       outputs: ArrayList<Tensor<*>>) -> Unit

inline fun <reified T> Project.buildTensor(outputs: Boolean = false): Tensor<T>
        where T : RealType<T>, T : NativeType<T> {

    val lines = extensions.getByName<ModelExt>("setModel")
            .dir.get().resolve("rdf.yaml").readText()
            .lines().map { it.trim() }
    val (x, y, z, w) = lines
            .dropWhile { it != "test_information:" }
            .dropWhile { it != "inputs:" }
            .first { it.startsWith("size: ") }
            .drop("size: ".length)
            .split(" x ")
            .map { it.toLong() }

    // Create an image that will be the backend of the Input Tensor
    //    val imgFactory_: ImgFactory<FloatType> = ArrayImgFactory(FloatType())
    //    val img1 = imgFactory.create(1, 1, 512, 512)

    val imgFactory/*: ImgFactory<T>*/ = when {
        T::class == FloatType::class -> ArrayImgFactory(FloatType())
        else -> error("invalid T ${T::class}")
    } as ImgFactory<T>
    val img = imgFactory.create(z, w, x, y)
    val target = lines.dropWhile { it != if (!outputs) "inputs:" else "outputs:" }
    val tensorName = target.first { it.startsWith("name: ") }.substring("name: ".length)
    val axes = target.first { it.startsWith("- axes: ") }.substring("- axes: ".length)
    return Tensor.build(tensorName, axes, img)
}