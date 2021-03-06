package com.github.tashoyan.recommender.stochastic

import scopt.OptionParser

trait StochasticGraphBuilderArgParser {

  val parser: OptionParser[StochasticGraphBuilderConfig] = new OptionParser[StochasticGraphBuilderConfig]("stochastic-graph-builder") {
    head("Stochastic Graph Builder")

    opt[String]("data-dir")
      .required()
      .valueName("<path>")
      .action((value, conf) => conf.copy(dataDir = value))
      .validate { value =>
        if (value.isEmpty) failure("Data directory must be non-empty path")
        else success
      }
      .text("Data directory to put the generated samples")

    opt[Int]("last-days-count")
      .required()
      .valueName("<number>")
      .action((value, conf) => conf.copy(lastDaysCount = value))
      .validate { value =>
        if (value <= 0) failure("Last days count must be positive")
        else success
      }
      .text("Consider past visits occurred during this number of last days")

    opt[Double]("beta-person-place")
      .required()
      .valueName("<value>")
      .action((value, conf) => conf.copy(betaPersonPlace = value))
      .validate { value =>
        if (value <= 0 || value >= 1) failure("Balancing coefficient must be in the interval (0; 1)")
        else success
      }
      .text("Balancing coefficient for 'person -> place' edges." +
        " Tells how much preference a person gives to a particular place" +
        " Sum of balancing coefficient for 'person -> place' and 'person -> category' edges must be 1.0")

    opt[Double]("beta-person-category")
      .required()
      .valueName("<value>")
      .action((value, conf) => conf.copy(betaPersonCategory = value))
      .validate { value =>
        if (value <= 0 || value >= 1) failure("Balancing coefficient must be in the interval (0; 1)")
        else success
      }
      .text("Balancing coefficient for 'person -> category' edges." +
        " Tells how much preference a person gives to a place category rather than a particular place" +
        " Sum of balancing coefficient for 'person -> place' and 'person -> category' edges must be 1.0")

    checkConfig { config =>
      if (config.betaPersonPlace + config.betaPersonCategory != 1.0)
        failure(
          s"Sum of balancing coefficients must be 1.0: beta-person-place: ${config.betaPersonPlace}, beta-person-category: ${config.betaPersonCategory}"
        )
      else
        success
    }

    help("help")
    version("version")

  }

}
