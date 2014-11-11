package ch.ethz.dal.dbcfw.utils

import ch.ethz.dal.dbcfw.regression.LabeledObject
import breeze.linalg.max
import breeze.linalg.DenseVector
import breeze.linalg.DenseMatrix
import breeze.linalg.VectorBuilder
import ch.ethz.dal.dbcfw.regression.LabeledObject
import breeze.linalg.CSCMatrix

object DissolveUtils {

  def loadLibSVMBinaryFile(filename: String, sparse: Boolean = true, labelMap: Map[String, Int]): DenseVector[LabeledObject] = {
    var n: Int = 0
    var ndims: Int = 0

    // Do some initial checks on labelMap
    require(labelMap.values.toList.contains(1), "labelMap (%s) contains no mapping to 1".format(labelMap))
    require(labelMap.values.toList.contains(-1), "labelMap (%s) contains no mapping to -1".format(labelMap))
    require(labelMap.values.toList.filter(x => (x != 1) && (x != -1)).size == 0, "labelMap (%s) contains a mapping to something other than a +1/-1".format(labelMap))

    // First pass, get number of data points and number of features
    for (
      line <- scala.io.Source.fromFile(filename).getLines()
        .map(_.trim)
        .filter(line => !(line.isEmpty || line.startsWith("#")))
    ) {
      n += 1
      ndims = max(ndims, max(line.split(" ").slice(1, line.split(" ").size).map(s => s.split(":")(0) toInt)))
    }

    // Second pass. Create a Vector of LabeledObjects
    val data: DenseVector[LabeledObject] = DenseVector.fill(n) { null }
    for (
      (line, idx) <- scala.io.Source.fromFile(filename).getLines()
        .map(_.trim)
        .filter(line => !(line.isEmpty || line.startsWith("#")))
        .zipWithIndex
    ) {

      // Create a Sparse Matrix by default. Later call the toDense method, in case we need a Dense Matrix instead
      val patternMatrixBuilder =
        new CSCMatrix.Builder[Double](rows = 1, cols = ndims)

      // Store label as a single-element dense vector
      val content: Array[String] = line.split(" ")
      val label = labelMap(content(0))
      val labelVector = DenseVector.fill(1) { label.toDouble }

      if (!(label == +1 || label == -1))
        throw new IllegalArgumentException("labelAdapter need to evalute to +1 or -1. Found %d.".format(label))

      content.slice(1, content.size)
        .map(pairStr => pairStr.split(":")) // Split "x:y"
        .map(pair => (pair(0).toInt, pair(1).toDouble)) // x is the index and y is the corresponding value
        .map {
          case (idx, value) =>
            patternMatrixBuilder.add(0, idx, value)
        }

      data(idx) = new LabeledObject(labelVector,
        if (sparse)
          patternMatrixBuilder.result()
        else
          patternMatrixBuilder.result().toDense)
    }

    println("Dataset size = %d".format(data.length))

    data
  }

  def main(args: Array[String]): Unit = {}

}