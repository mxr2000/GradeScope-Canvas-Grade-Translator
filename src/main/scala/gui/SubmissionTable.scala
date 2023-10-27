package gui

import model._
import scalafx.beans.property._
import scalafx.scene.control._
import scalafx.collections._
import scalafx.beans.binding.Bindings
import scalafx.scene.layout._
import scalafx.geometry.Insets
import scalafx.scene.text.Text
import scalafx.scene.AccessibleRole.CheckBox
import scalafx.beans.property.ObjectProperty
import javafx.scene.control as jfxsc
import scalafx.collections.transformation.FilteredBuffer
import scalafx.scene.shape.Rectangle
import scalafx.scene.paint.Color
import scalafx.beans.binding._
import javafx.{collections => jfxc, event => jfxe, scene => jfxs, util => jfxu}
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
    status: Option[SubmissionStatus]
) {
  def nameProperty = StringProperty(gs.name)
  def scoreProperty = StringProperty(gs.score)
  def timeProperty = ObjectProperty(gs.time)
  def gradeScopeLinkProperty = StringProperty(gs.link)
  def canvasLinkProperty = StringProperty(cs.map(_.link).getOrElse("?"))
  def translatedProperty = ObjectProperty[Option[Boolean]](translated)
  def gradedProperty = ObjectProperty[Option[Boolean]](graded)
  def statusProperty = ObjectProperty[Option[SubmissionStatus]](status)
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
  val showOtherStatusChecked = BooleanProperty(true)

  val sortByTimeSelected = BooleanProperty(true)
  val sortByNameSelected = BooleanProperty(false)
  val sortByScoreSelected = BooleanProperty(false)

  val sortTog = new ToggleGroup

  val translatedGroup = new VBox {
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

  val gradedGroup = new VBox {
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

  val statusGroup = new VBox {
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

  val sortGroups = new VBox {
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

  def filterGradedRow(s: SubmissionRow) =
    showGradedChecked.value && s.graded == Some(true) ||
      showNotGradedChecked.value && s.graded != Some(true)

  def filterTranslatedRow(s: SubmissionRow) =
    showTranslatedChecked.value && s.translated == Some(true) ||
      showNotTranslatedChecked.value && s.translated != Some(true)

  def filterStatusRow(s: SubmissionRow) =
    true

  val bindings =
    Bindings.createObjectBinding[jfxc.ObservableList[SubmissionRow]](
      () => {
        val temp = submissionRows
          .filter { filterGradedRow }
          .filter { filterTranslatedRow }
          .filter { filterStatusRow }
          .sortBy { s =>
            (
              sortByNameSelected.value,
              sortByScoreSelected.value
            ) match
              case (true, _) => s.nameProperty.value
              case (_, true) => s.scoreProperty.value
              case _         => s.nameProperty.value
          }
        if sortByTimeSelected.value
        then temp.sortBy { _.timeProperty.value }
        else temp
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

  val tableView = new TableView[SubmissionRow] {
    vgrow = Priority.ALWAYS
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
          cell.graphic = Hyperlink(s"https://canvas.its.virginia.edu${link}")
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
          cell.alignment = Pos.CENTER
        }
      },
      new TableColumn[SubmissionRow, Option[Boolean]] {
        text = "Translated"
        cellValueFactory = _.value.translatedProperty
        prefWidth = 100
        cellFactory = { (cell, v) =>
          cell.graphic = new Rectangle {
            width = 10
            height = 10
            fill = v match
              case None        => Color.Gray
              case Some(true)  => Color.Green
              case Some(false) => Color.Red
          }
          cell.alignment = Pos.CENTER
        }
      },
      new TableColumn[SubmissionRow, Option[SubmissionStatus]] {
        text = "Status"
        cellValueFactory = _.value.statusProperty
        prefWidth = 100
      },
      new TableColumn[SubmissionRow, String] {
        text = "Canvas Link"
        cellValueFactory = _.value.canvasLinkProperty
        prefWidth = 100
        cellFactory = { (cell, link) =>
          cell.graphic = new Hyperlink {
            text = "link"
            onAction =
              (_) => openWebpage(s"https://canvas.its.virginia.edu${link}")
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

  val accordion = new Accordion {
    panes = Seq(
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

  val view = new HBox {
    children = Seq(
      accordion,
      tableView
    )
    hgrow = Priority.ALWAYS
    vgrow = Priority.ALWAYS
    spacing = 10
  }
}
