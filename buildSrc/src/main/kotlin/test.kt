import io.bioimage.modelrunner.bioimageio.description.ModelDescriptor
import java.nio.file.Path

@BuilderDsl
inline fun framework(init: FrameworkBuilder.() -> Unit) {
    FrameworkBuilder().apply(init)
}

class FrameworkBuilder {

}

@BuilderDsl
inline fun model(init: ModelBuilder.() -> Unit) {
    ModelBuilder().apply(init)
}

class ModelBuilder {
//    lateinit var model: Models
//    var dir :
}

val models = mutableMapOf<Path, ModelDescriptor>()

@DslMarker
annotation class BuilderDsl