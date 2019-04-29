package com.github.tashoyan.visitor.recommender.stochastic

import com.github.tashoyan.visitor.recommender.{DataUtils, PlaceVisits}
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.types.LongType
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}

object StochasticGraphBuilderMain extends StochasticGraphBuilderArgParser with PlaceVisits {

  private val betaPlacePlace: Double = 1.0
  private val betaCategoryPlace: Double = 1.0

  def main(args: Array[String]): Unit = {
    parser.parse(args, StochasticGraphBuilderConfig()) match {
      case Some(config) => doMain(config)
      case None => sys.exit(1)
    }
  }

  private def doMain(implicit config: StochasticGraphBuilderConfig): Unit = {
    implicit val spark: SparkSession = SparkSession.builder()
      .getOrCreate()

    Console.out.println(s"Actual configuration: $config")

    val locationVisits = spark.read
      .parquet(s"${config.samplesDir}/location_visits_sample")
      .withColumn("region_id", col("region_id") cast LongType)
    val places = spark.read
      .parquet(s"${config.samplesDir}/places_sample")
      .withColumn("region_id", col("region_id") cast LongType)

    Console.out.println("Generating place visits")
    val placeVisits = calcPlaceVisits(locationVisits, places)
      .cache()
    printPlaceVisits(placeVisits)
    writePlaceVisits(placeVisits, config.samplesDir)

    generateRegionGraphs(placeVisits)
  }

  private def generateRegionGraphs(placeVisits: DataFrame)(implicit spark: SparkSession, config: StochasticGraphBuilderConfig): Unit = {
    val regionsPlaceVisits = extractRegionsPlaceVisits(placeVisits)

    val regionStochasticGraphs = regionsPlaceVisits
      .map { case (regIds, regPlaceVisits) =>
        (DataUtils.generateGraphFileName(regIds, config.samplesDir), generateStochasticGraph(regPlaceVisits))
      }
    regionStochasticGraphs.foreach { case (fileName, graph) =>
      Console.out.println(s"Writing stochastic graph : $fileName")
      writeStochasticGraph(fileName, graph)
    }
  }

  private def generateStochasticGraph(placeVisits: DataFrame)(implicit config: StochasticGraphBuilderConfig): DataFrame = {
    val placeSimilarPlaceEdges = PlaceSimilarPlace.calcPlaceSimilarPlaceEdges(placeVisits)
    val categorySelectedPlaceEdges = CategorySelectedPlace.calcCategorySelectedPlaceEdges(placeVisits)
    val personLikesPlaceEdges = PersonLikesPlace.calcPersonLikesPlaceEdges(placeVisits)
    val personLikesCategoryEdges = PersonLikesCategory.calcPersonLikesCategoryEdges(placeVisits)
    val allEdges = Seq(
      placeSimilarPlaceEdges,
      categorySelectedPlaceEdges,
      personLikesPlaceEdges,
      personLikesCategoryEdges
    )
    val betas = Seq(
      betaPlacePlace,
      betaCategoryPlace,
      config.betaPersonPlace,
      config.betaPersonCategory
    )
    val stochasticGraph = StochasticGraphBuilder.buildWithBalancedWeights(betas, allEdges)
    stochasticGraph
  }

  private def writeStochasticGraph(fileName: String, graph: DataFrame): Unit = {
    graph.write
      .mode(SaveMode.Overwrite)
      .parquet(fileName)
  }

}
