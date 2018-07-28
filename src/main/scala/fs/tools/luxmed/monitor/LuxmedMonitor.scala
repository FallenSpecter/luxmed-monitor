package fs.tools.luxmed.monitor

import java.io.File
import java.nio.file.Paths

import io.circe.generic.auto._
import io.circe.parser._
import org.slf4j.LoggerFactory

import scala.io.Source

object LuxmedMonitor extends App {

  private final val Logger = LoggerFactory.getLogger(LuxmedMonitor.getClass)
  private final val ConfigFilename = "luxmed-monitor.config.json"

  runApplication match {
    case Right(_) => Logger.info("Success. Appointment slot was found!")
    case Left(error) => Logger.error("Error occurred while running the application", error)
  }

  private def runApplication: Either[Throwable, Unit] = for {
    configFile <- findConfigFile
    config <- extractConfig(configFile)
  } yield new Crawler(config).run()

  private def findConfigFile: Either[Throwable, File] =
    findConfigFileInCurrentWorkingDirectory
      .orElse(findConfigFileInUserDirectory)
      .toRight(throw new NoSuchElementException)

  private def findConfigFileInCurrentWorkingDirectory: Option[File] = {
    Logger.info("Checking current working directory for config file")
    val file = Paths.get(sys.props("user.dir"), ConfigFilename).toFile
    if (file.exists()) Some(file) else None
  }

  private def findConfigFileInUserDirectory: Option[File] = {
    Logger.info("Checking user home directory for config file")
    val file = Paths.get(sys.props("user.home"), ConfigFilename).toFile
    if (file.exists()) Some(file) else None
  }

  private def extractConfig(file: File): Either[Throwable, Config] =
    decode[Config](Source.fromFile(file).mkString)

}

