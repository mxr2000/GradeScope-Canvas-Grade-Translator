import cats.Applicative
import cats.data.EitherT
import scalafx.application.JFXApp3
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.effect.*
import scalafx.scene.layout.*
import scalafx.scene.paint.Color.*
import scalafx.scene.paint.*
import scalafx.scene.text.Text
import scalafx.collections.*
import scalafx.scene.control.*
import scalafx.Includes.eventClosureWrapperWithZeroParam
import scalafx.event.EventIncludes.eventClosureWrapperWithZeroParam
import scalafx.beans.property.*
import javafx.scene.control as jfxsc

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import concurrent.ExecutionContext.Implicits.global
import scalafx.scene.control.Alert.AlertType

import scala.util.*
import scalafx.beans.binding.Bindings
import model.*
import gui.*
import scalafx.geometry.Pos
import cats.Apply

object Gui extends JFXApp3 {

  val submissionRows: ObservableBuffer[SubmissionRow] =
    ObservableBuffer[SubmissionRow]()
  private val gradeProgress = DoubleProperty(0)
  private val loadingProgress = DoubleProperty(0)

  private def startGrading(dialogResult: DialogResult): Unit = {
    SettingsDao.createInstances.fold(
      err => println(err),
      (canvas, _) => {
        def postQuizGrade(
            link: String,
            grade: String,
            comment: String,
            oldRow: SubmissionRow,
            status: SubmissionStatus
        ): SubmissionRow =
          canvas
            .postQuizGrade(
              link,
              grade,
              comment
            )
            .fold(
              err =>
                println(s"${grade} ${err}")
                oldRow
                  .copy(
                    translated = Some(false),
                    status = Some(SubmissionStatus.SubmissionFailed(err))
                  ),
              _ =>
                println(s"${oldRow.gs.name}:${grade}")
                oldRow
                  .copy(
                    translated = Some(true),
                    status = Some(status)
                  )
            )

        import cats.implicits._
        import scala.concurrent.ExecutionContext

        implicit val customExecutionContext: ExecutionContext =
          ExecutionContext.fromExecutor(
            java.util.concurrent.Executors.newFixedThreadPool(5)
          )

        val futureTasks = List
          .range(0, submissionRows.size)
          .filter { index => submissionRows(index).graded.contains(true) }
          .map { index =>
            val oldRow = submissionRows(index)
            val newRow = oldRow.cs match
              case None =>
                Future {
                  oldRow.copy(status =
                    Some(SubmissionStatus.SubmissionDoesNotSubmitOnCanvas),
                    translated = Some(false)
                  )
                }
              case Some(cs) =>
                val gs = oldRow.gs
                val score = if dialogResult.gradeManual then oldRow.manualScore.getOrElse("0.0") else gs.score
                canvas
                  .getSubmissionStatus(cs.link, gs.time)
                  .map {
                    case status @ SubmissionStatus.SubmissionTimeOut(_) =>
                      postQuizGrade(
                        cs.link,
                        "0.0",
                        dialogResult.timeOutComment,
                        oldRow,
                        status
                      )
                    case status @ SubmissionStatus
                          .SubmissionDoesNotMatch(_, _) =>
                      postQuizGrade(
                        cs.link,
                        "0.0",
                        dialogResult.timeDoesNotMatchComment,
                        oldRow,
                        status
                      )
                    case status @ SubmissionStatus.SubmissionDoesNotSubmitOnCanvas =>
                      postQuizGrade(
                        cs.link,
                        "0.0",
                        dialogResult.noSubmissionComment,
                        oldRow,
                        status
                      )
                    case status @ SubmissionStatus
                          .SubmissionMultipleTimes(_, _) =>
                      postQuizGrade(
                        cs.link,
                        score,
                        "",
                        oldRow,
                        status
                      )

                    case status @ SubmissionStatus.SubmissionSuccess =>
                      postQuizGrade(
                        cs.link,
                        score,
                        "",
                        oldRow,
                        status
                      )

                    case status =>
                      oldRow
                        .copy(translated = Some(false), status = Some(status))
                  }

            newRow.map { nr =>
              submissionRows.update(index, nr)
              gradeProgress.update((index + 1) * 1.0 / submissionRows.size)
            }
          }
          .sequence

        futureTasks.onComplete {
          case scala.util.Success(_) =>
            println("All tasks completed successfully")
          case scala.util.Failure(ex) => println(s"An error occurred: $ex")
        }
      }
    )

  }

  override def start(): Unit = {
    stage = new JFXApp3.PrimaryStage {
      title = "Grade Translator"

      val btnShowDialog: Button = new Button {
        text = "Translate Grades"
        onAction = { _ =>
          gui.PostSettingsDialog.showDialog(stage) match
            case None        => println()
            case Some(value) => startGrading(value)
        }
      }

      val btnAdjustActivation: Button = new Button {
        text = "Adjust Activation"
      }

      val btnLoad: Button = new Button {
        text = "Load"
        onAction = { * => loadGradeScopeScores() }
      }

      val btnSettings: Button = new Button {
        text = "Settings"
        onAction = { * =>
          gui.SettingsDialog.showDialog(stage) match
            case Some(_) => println("")
            case None    => println("")
        }
      }

      val loadingIndicator = new ProgressIndicator {
        progress <==> loadingProgress
      }

      val root: VBox = new VBox {
        margin = Insets(10)
        padding = Insets(10)
        spacing = 10
        children = Seq(
          new HBox {
            children = Seq(
              btnShowDialog,
              btnAdjustActivation,
              btnLoad,
              btnSettings,
              loadingIndicator
            )
            spacing = 10
          },
          SubmissionTable(submissionRows).view,
          new HBox {
            children = Seq(
              new Label {
                text = "Status: "
              },
              new ProgressBar {
                progress <==> gradeProgress
                prefWidth = 1000
              }
            )
            hgrow = Priority.Always
          }
        )
      }

      def loadGradeScopeScores(): Unit = {
        import org.scalafx.extras._
        loadingProgress.update(-1)
        SettingsDao.createInstances.fold(
          err =>
            onFX {
              new Alert(AlertType.Information, s"Error: ${err}!!!")
                .showAndWait()
            },
          (canvas, gradeScope) => {
            import cats.instances.future._
            import cats.syntax.all._
            import cats.instances.all._

            val task = (
              gradeScope.getManualGradingList,
              gradeScope.getScores,
              canvas.getStudentSubmission()
            ).parMapN { (manualGraded, scores, submissionList) =>
              val manualNameSet = manualGraded.map(_._1).toSet
              val manualMap = manualGraded.toMap
              scores.map { (name, gs) =>
                SubmissionRow(
                  gs,
                  submissionList.get(name),
                  if manualNameSet.contains(name) then Some(true) else Some(false),
                  None,
                  None,
                  manualMap.get(name)
                )
              }
            }

            val result = task
              .fold(
                err =>
                  Future {
                    println(err)
                    onFX {
                      new Alert(AlertType.Information, s"${err}!!!")
                        .showAndWait()
                    }
                  },
                scores =>
                  submissionRows.clear()
                  submissionRows ++= scores.toList
                  Future {
                    onFX {
                      loadingProgress.update(1)
                    }
                  }
              )
              .flatten

            result.onComplete {
              case Success(_) =>
                println("All operations completed successfully")
              case Failure(ex) =>
                println(s"An error occurred: ${ex.getMessage}")
            }
          }
        )

      }

      scene = new Scene(root, 1200, 800)
    }
  }
}
