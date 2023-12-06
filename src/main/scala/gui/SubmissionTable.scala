package gui

import javafx.collections.ObservableList
import model.*
import scalafx.beans.property.*
import scalafx.scene.control.{cell, *}
import scalafx.collections.*
import scalafx.beans.binding.Bindings
import scalafx.scene.layout.*
import scalafx.geometry.Insets
import scalafx.scene.text.{Text, TextAlignment}
import scalafx.scene.AccessibleRole.CheckBox
import scalafx.beans.property.ObjectProperty
import javafx.scene.control as jfxsc
import scalafx.collections.transformation.FilteredBuffer
import scalafx.scene.shape.Rectangle
import scalafx.scene.paint.Color
import scalafx.beans.binding.*
import javafx.{collections as jfxc, event as jfxe, scene as jfxs, util as jfxu}

import java.net.URI
import scalafx.geometry.Pos

import java.time.LocalDateTime
import java.time.ZoneOffset
import scalafx.scene.layout.HBox
import scalafx.stage.Stage

case class SubmissionRow(
    gs: GradeScopeSubmission,
    cs: Option[CanvasSubmission],
    graded: Option[Boolean],
    translated: Option[Boolean],
    status: Option[SubmissionStatus],
    manualScore: Option[String]
) {
  def nameProperty = StringProperty(gs.name)
  def scoreProperty = StringProperty(gs.score)
  def timeProperty = ObjectProperty(gs.time)
  def gradeScopeLinkProperty = StringProperty(gs.link)
  def canvasLinkProperty = StringProperty(cs.map(_.link).getOrElse("?"))
  def translatedProperty = ObjectProperty[Option[Boolean]](translated)
  def gradedProperty = ObjectProperty[Option[Boolean]](graded)
  def statusProperty = ObjectProperty[Option[SubmissionStatus]](status)

  def manualScoreProperty = ObjectProperty[Option[String]](manualScore)
}

enum SortStrategy:
  case ByName extends SortStrategy
  case ByTime extends SortStrategy
  case ByScore extends SortStrategy

final case class SubmissionTable(
    submissionRows: ObservableBuffer[SubmissionRow]
) {
  val showGradedChecked = BooleanProperty(true)
  val showNotGradedChecked = BooleanProperty(true)
  val showTranslatedChecked = BooleanProperty(true)
  val showNotTranslatedChecked = BooleanProperty(true)

  val showPostSuccessChecked = BooleanProperty(true)
  val showSubmissionTimeOutChecked = BooleanProperty(true)
  val showSubmissionTimeDoesNotMatchChecked = BooleanProperty(true)
  val showNoSubmissionOnGradeScopeChecked = BooleanProperty(true)
  val showNoSubmissionOnCanvasChecked = BooleanProperty(true)
  val showPostFailedChecked = BooleanProperty(true)
  val showSubmissionMultipleTimesChecked = BooleanProperty(true)
  val showOtherStatusChecked = BooleanProperty(true)

  val sortByTimeSelected = BooleanProperty(true)
  val sortByNameSelected = BooleanProperty(false)
  val sortByScoreSelected = BooleanProperty(false)

  val sortTog = new ToggleGroup

  val translatedGroup: VBox = new VBox {
    children = Seq(
      new CheckBox {
        text = "Show Translated"
        selected <==> showTranslatedChecked
      },
      new CheckBox {
        text = "Show Not Translated"
        selected <==> showNotTranslatedChecked
      }
    )
    padding = Insets(10)
    spacing = 10
  }

  val gradedGroup: VBox = new VBox {
    children = Seq(
      new CheckBox {
        text = "Show Graded"
        selected <==> showGradedChecked
      },
      new CheckBox {
        text = "Show Not Graded"
        selected <==> showNotGradedChecked
      }
    )
    padding = Insets(10)
    spacing = 10
  }

  private val statusGroup = new VBox {
    children = Seq(
      new CheckBox {
        text = "Post Success"
        selected <==> showPostSuccessChecked
      },
      new CheckBox {
        text = "No Submission On GradeScope"
        selected <==> showNoSubmissionOnGradeScopeChecked
      },
      new CheckBox {
        text = "No Submission On Canvas"
        selected <==> showNoSubmissionOnCanvasChecked
      },
      new CheckBox {
        text = "Submission TimeOut"
        selected <==> showSubmissionTimeOutChecked
      },
      new CheckBox {
        text = "Time Does Not Match"
        selected <==> showSubmissionTimeDoesNotMatchChecked
      },
      new CheckBox {
        text = "Submit Twice"
        selected <==> showSubmissionMultipleTimesChecked
      },
      new CheckBox {
        text = "Post Failed"
        selected <==> showPostFailedChecked
      },
      new CheckBox {
        text = "Others"
        selected <==> showOtherStatusChecked
      }
    )
    padding = Insets(10)
    spacing = 10
  }

  val sortGroups: VBox = new VBox {
    children = Seq(
      new RadioButton {
        text = "time"
        toggleGroup = sortTog
        userData = SortStrategy.ByName
        selected <==> sortByTimeSelected
      },
      new RadioButton {
        text = "score"
        toggleGroup = sortTog
        userData = SortStrategy.ByName
        selected <==> sortByScoreSelected
      },
      new RadioButton {
        text = "name"
        toggleGroup = sortTog
        userData = SortStrategy.ByName
        selected <==> sortByNameSelected
      }
    )
    padding = Insets(10)
    spacing = 10
  }

  private def filterGradedRow(s: SubmissionRow) =
    showGradedChecked.value && s.graded.contains(true) ||
      showNotGradedChecked.value && !s.graded.contains(true)

  private def filterTranslatedRow(s: SubmissionRow) =
    showTranslatedChecked.value && s.translated.contains(true) ||
      showNotTranslatedChecked.value && !s.translated.contains(true)

  private def filterStatusRow(s: SubmissionRow) = s.status match {
    case None => showOtherStatusChecked.value
    case Some(status) => status match
      case SubmissionStatus.SubmissionTimeOut(_) => showSubmissionTimeOutChecked.value
      case SubmissionStatus.SubmissionDoesNotMatch(_, _) => showSubmissionTimeDoesNotMatchChecked.value
      case SubmissionStatus.SubmissionDoesNotSubmitOnCanvas => showNoSubmissionOnCanvasChecked.value
      case SubmissionStatus.SubmissionFailed(_) => showPostFailedChecked.value
      case SubmissionStatus.SubmissionSuccess => showPostSuccessChecked.value
      case SubmissionStatus.SubmissionMultipleTimes(_, _) => showSubmissionTimeOutChecked.value
  }

  val bindings: ObjectBinding[ObservableList[SubmissionRow]] =
    Bindings.createObjectBinding[jfxc.ObservableList[SubmissionRow]](
      () => {
        val temp = submissionRows
          .filter { filterGradedRow }
          .filter { filterTranslatedRow }
          .filter { filterStatusRow }

        if sortByTimeSelected.value
        then temp.sortBy { _.timeProperty.value }
        else if sortByScoreSelected.value
        then temp.sortBy { _.scoreProperty.value.toFloatOption.getOrElse(0f) }
        else temp.sortBy { _.nameProperty.value }
      },
      submissionRows,
      showGradedChecked,
      showNotGradedChecked,
      showTranslatedChecked,
      showNotTranslatedChecked,
      sortByNameSelected,
      sortByScoreSelected,
      sortByTimeSelected,
      showNoSubmissionOnCanvasChecked,
      showNoSubmissionOnGradeScopeChecked,
      showPostSuccessChecked,
      showPostFailedChecked,
      showSubmissionTimeOutChecked,
      showSubmissionTimeDoesNotMatchChecked,
      showOtherStatusChecked
    )

  val tableView: TableView[SubmissionRow] = new TableView[SubmissionRow] {
    vgrow = Priority.Always
    items <== bindings

    columns ++= List(
      new TableColumn[SubmissionRow, String] {
        text = "Name"
        cellValueFactory = _.value.nameProperty
        prefWidth = 100
      },
      new TableColumn[SubmissionRow, String] {
        text = "Score"
        cellValueFactory = _.value.scoreProperty
        prefWidth = 50
      },
      new TableColumn[SubmissionRow, LocalDateTime] {
        text = "Time"
        cellValueFactory = _.value.timeProperty
        prefWidth = 150
      },
      new TableColumn[SubmissionRow, String] {
        text = "GradeScope Link"
        cellValueFactory = _.value.gradeScopeLinkProperty
        prefWidth = 100
        cellFactory = { (cell, link) =>
          cell.graphic = new Hyperlink {
            text = "link"
            onAction = _ => openWebpage(s"https://www.gradescope.com$link")
          }
        }
      },
      new TableColumn[SubmissionRow, Option[Boolean]] {
        text = "Manual Graded"
        prefWidth = 100
        cellValueFactory = _.value.gradedProperty
        cellFactory = { (cell, v) =>
          cell.graphic = new Rectangle {
            width = 10
            height = 10
            fill = v match
              case None        => Color.Gray
              case Some(true)  => Color.Green
              case Some(false) => Color.Red
          }
          cell.alignment = Pos.Center
        }
      },
      new TableColumn[SubmissionRow, Option[Boolean]] {
        text = "Translated"
        cellValueFactory = _.value.translatedProperty
        prefWidth = 100
        cellFactory = { (cell, v) =>
          cell.graphic = new Rectangle {
            width = 20
            height = 20
            fill = v match
              case None        => Color.White
              case Some(true)  => Color.Green
              case Some(false) => Color.Gray
          }
          cell.alignment = Pos.Center

        }
      },
      new TableColumn[SubmissionRow, Option[String]] {
        text = "Manual Score"
        cellValueFactory = _.value.manualScoreProperty
        prefWidth = 100
        cellFactory = { (cell, v) =>
          cell.graphic = new Text {
            text = v.getOrElse("")
          }
          cell.alignment = Pos.Center
        }
      },
      new TableColumn[SubmissionRow, Option[SubmissionStatus]] {
        text = "Status"
        cellValueFactory = _.value.statusProperty
        prefWidth = 150
        cellFactory = { (cell, status) =>
          cell.graphic = new StackPane {
            children = Seq(
              new Rectangle {
                width = 80
                height = 20
                arcWidth = 2
                arcHeight = 2
                fill = status match
                  case None => Color.Gray
                  case Some(s) =>
                    s match
                      case SubmissionStatus.SubmissionSuccess    => Color.Green
                      case SubmissionStatus.SubmissionTimeOut(_) => Color.Yellow
                      case SubmissionStatus.SubmissionFailed(_)  => Color.Red
                      case SubmissionStatus.SubmissionDoesNotMatch(_, _) =>
                        Color.Orange
                      case SubmissionStatus.SubmissionMultipleTimes(_, _) =>
                        Color.LightGreen
                      case SubmissionStatus.SubmissionDoesNotSubmitOnCanvas =>
                        Color.Brown
              },
              new Text {
                style = "-fx-font: 12 arial;"
                textAlignment = TextAlignment.Center
                text = status match
                  case None => "None"
                  case Some(value) =>
                    value match
                      case SubmissionStatus.SubmissionSuccess => "Success"
                      case SubmissionStatus.SubmissionFailed(err) =>
                        s"Post Failed($err)"
                      case SubmissionStatus.SubmissionTimeOut(duration) =>
                        s"Time out($duration)"
                      case SubmissionStatus.SubmissionDoesNotMatch(_, _) =>
                        s"Does not match"
                      case SubmissionStatus.SubmissionDoesNotSubmitOnCanvas =>
                        s"Not on Canvas"
                      case SubmissionStatus.SubmissionMultipleTimes(_, _) =>
                        s"Multiple times"
              }
            )
          }
        }
      },
      new TableColumn[SubmissionRow, String] {
        text = "Canvas Link"
        cellValueFactory = _.value.canvasLinkProperty
        prefWidth = 100
        cellFactory = { (cell, link) =>
          cell.graphic = new Hyperlink {
            text = "link"
            onAction = _ => openWebpage(s"https://canvas.its.virginia.edu$link")
          }
        }
      }
    )
  }

  private def openWebpage(urlString: String): Unit = {
    try {
      val uri = new URI(urlString)
      java.awt.Desktop.getDesktop.browse(uri)
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }

  private val accordion = new VBox() {
    spacing = 10
    children = Seq(
      new TitledPane {
        text = "Status"
        content = statusGroup
      },
      new TitledPane {
        text = "Graded"
        content = gradedGroup
      },
      new TitledPane {
        text = "Translated"
        content = translatedGroup
      },
      new TitledPane {
        text = "Sort"
        content = sortGroups
      }
    )

  }

  val view: HBox = new HBox {
    children = Seq(
      accordion,
      tableView
    )
    hgrow = Priority.Always
    vgrow = Priority.Always
    spacing = 10
  }
}
