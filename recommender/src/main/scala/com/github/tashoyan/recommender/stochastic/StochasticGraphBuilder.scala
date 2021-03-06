package com.github.tashoyan.recommender.stochastic

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.col

object StochasticGraphBuilder {

  def buildWithBalancedWeights(betas: Seq[Double], allEdges: Seq[DataFrame]): DataFrame = {
    val firstBeta = betas.head
    val firstEdges = allEdges.head
    val firstGraph = firstEdges
      .select(
        col("source_id"),
        col("target_id"),
        col("weight") * firstBeta as "balanced_weight"
      )
    val otherBetas = betas.tail
    val otherEdges = allEdges.tail
    (otherEdges zip otherBetas).foldLeft(firstGraph) { case (graph, (edges, beta)) =>
      graph union
        edges
        .select(
          col("source_id"),
          col("target_id"),
          col("weight") * beta as "balanced_weight"
        )
    }
  }

}
