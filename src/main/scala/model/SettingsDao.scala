package model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.*
import io.circe.parser.decode
import io.circe.parser.parse

import java.io.{BufferedWriter, File, FileWriter}
import scala.io.Source
import scala.util.Try
case class CanvasSettings(
    courseId: String,
    quizId: String,
    questionId: String,
    cookies: String,
    token: String
)

case class GradeScopeSettings(
    courseId: String,
    assignmentId: String,
    questionId: String,
    cookies: String,
    token: String
)

case class Settings(
    canvasSettings: CanvasSettings,
    gradeScopeSettings: GradeScopeSettings
)

object SettingsDao {
  given canvasDecoder: Decoder[CanvasSettings] =
    deriveDecoder[CanvasSettings]

  given canvasEncoder: Encoder[CanvasSettings] =
    deriveEncoder[CanvasSettings]

  given gradeScopeDecoder: Decoder[GradeScopeSettings] =
    deriveDecoder[GradeScopeSettings]

  given gradeScopeEncoder: Encoder[GradeScopeSettings] =
    deriveEncoder[GradeScopeSettings]

  given settingsDecoder: Decoder[Settings] = deriveDecoder[Settings]

  given settingsEncoder: Encoder[Settings] = deriveEncoder[Settings]

  private def loadFileAsString(filePath: String): Either[String, String] = {
    Try {
      val source = Source.fromFile(filePath)
      val jsonString = source.mkString
      source.close()
      jsonString
    }.fold(
      ex => Left(ex.getMessage),
      value => Right(value)
    )
  }

  def loadSettings(filePath: String): Either[String, Settings] = {
    loadFileAsString(filePath).flatMap {
      decode[Settings](_).fold(
        ex => Left(ex.getMessage),
        value => Right(value)
      )
    }
  }

  def saveSettings(filePath: String, settings: Settings): Unit = {
    val file = new File(filePath)
    val writer = new BufferedWriter(new FileWriter(file))
    try {
      writer.write(Encoder[Settings].apply(settings).toString)
    } finally {
      writer.close()
    }
  }

  def createInstances: Either[String, (Canvas, GradeScope)] = {
    loadSettings("a.json").map { s =>
      val cs = s.canvasSettings
      val gs = s.gradeScopeSettings
      (
        Canvas(cs.courseId, cs.quizId, cs.questionId, cs.cookies, cs.token),
        GradeScope(gs.courseId, gs.assignmentId, gs.questionId, gs.cookies, gs.token)
      )
    }
  }

  /*@main
  def main = {
    val settings: Settings = Settings(
      canvasSettings = CanvasSettings("a", "b", "c", "d", "e"),
      gradeScopeSettings = GradeScopeSettings("a", "b", "c", "d")
    )
    saveSettings("a.json", settings)
  }*/
}
