import scalafx.application.JFXApp3
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.effect._
import scalafx.scene.layout._
import scalafx.scene.paint.Color._
import scalafx.scene.paint._
import scalafx.scene.text.Text
import scalafx.collections._
import scalafx.scene.control._
import scalafx.Includes.eventClosureWrapperWithZeroParam
import scalafx.event.EventIncludes.eventClosureWrapperWithZeroParam
import scalafx.beans.property.StringProperty
import scalafx.beans.property.ObjectProperty
import javafx.scene.control as jfxsc
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import concurrent.ExecutionContext.Implicits.global
import scalafx.scene.control.Alert.AlertType
import scala.concurrent.Future
import scala.util.Success
import scala.util.Failure
import scalafx.beans.property.BooleanProperty
import scalafx.beans.binding.Bindings
import model._
import gui._

object ScalaFXHelloWorld extends JFXApp3 {
  val gradeScope: GradeScope =
    GradeScope(
      courseId = "576725",
      questionId = "28044790",
      assignmentId = "3480775"
    )

  val canvas: Canvas = Canvas(
    courseId = "72193",
    quizId = "103054",
    questionId = "2075376"
  )

  override def start(): Unit = {
    stage = new JFXApp3.PrimaryStage {
      title = "Grade Translator"

      val submissionRows = ObservableBuffer[SubmissionRow]()

      val btnShowDialog = new Button {
        text = "Show Dialog"
        onAction = { _ =>
          gui.Dialog.showDialog(stage)
        }
      }

      val root = new VBox {
        margin = Insets(10)
        padding = Insets(10)
        spacing = 10
        children = Seq(
          new HBox {
            children = Seq(
              btnShowDialog
            )
          },
          SubmissionTable(submissionRows).view
        )
      }

      def loadGradeScopeScores(): Unit = {
        import org.scalafx.extras._
        val task = for {
          manualGraded <- gradeScope.getManualGradingList
          scores <- gradeScope.getScores
          submissionList <- canvas.getStudentSubmission()
        } yield {
          val manualSet = manualGraded.toSet
          scores.map { (name, gs) =>
            SubmissionRow(
              gs,
              submissionList.get(name),
              if manualSet.contains(name) then Some(true) else Some(false),
              None,
              None
            )
          }
        }
        val result = task
          .fold(
            err =>
              Future {
                onFX {
                  new Alert(AlertType.Information, s"${err}!!!").showAndWait()
                }
              },
            scores =>
              submissionRows.clear()
              submissionRows ++= scores.toList
              Future {
                onFX {
                  new Alert(AlertType.Information, s"${scores.size}")
                    .showAndWait()
                }
              }
          )
          .flatten

        result.onComplete {
          case Success(_)  => println("All operations completed successfully")
          case Failure(ex) => println(s"An error occurred: ${ex.getMessage}")
        }
      }

      scene = new Scene(root, 1100, 800)
    }
  }
}
