import net.imglib2.type.numeric.real.FloatType
import net.imglib2.util.Util

plugins { bioimage.io.jdll }

setEngine {
    engine = Framework.torchscript.`1,13,1`
}

setModel {
    model = Models.EnhancerMitochondriaEM2D
}

execute = { model, inputsTensor, outputsTensor ->

    inputsTensor += buildTensor<FloatType>()
    outputsTensor += buildTensor<FloatType>(outputs = true)

    // Run the model on the input tensors. THe output tensors
    // will be rewritten with the result of the execution
    println(Util.average(Util.asDoubleArray(outputsTensor.first().data)))
    model.runModel(inputsTensor, outputsTensor)
    println(Util.average(Util.asDoubleArray(outputsTensor.first().data)))
}