package gui

import scalafx.scene.control._
import scalafx.scene.layout._
import scalafx.stage.Stage
import scalafx.geometry.Insets
import scalafx.beans.property.StringProperty
import scalafx.scene.control.ButtonBar.ButtonData

case class DialogResult(
    noSubmissionComment: String,
    timeOutComment: String,
    timeDoesNotMatchComment: String,
    gradeManual: Boolean
)

object PostSettingsDialog {

  val noSubmissionComment = StringProperty("no submission on GradeScope")
  val timeOutComment = StringProperty("Timeout")
  val timeDoesNotMatchComment = StringProperty("Time Does not match on GradeScope")

  val noSubmissionCommentInput = new TextField {
    text <==> noSubmissionComment
  }
  val timeOutCommentInput = new TextField {
    text <==> timeOutComment
  }
  val timeDoesNotMatchCommentInput = new TextField {
    text <==> timeDoesNotMatchComment
  }

  val grid: GridPane = new GridPane() {
    hgap = 10
    vgap = 10
    padding = Insets(20, 100, 10, 10)

    add(new Label("No Submission Comment:"), 0, 0)
    add(noSubmissionCommentInput, 1, 0)
    add(new Label("Time Out Comment:"), 0, 1)
    add(timeOutCommentInput, 1, 1)
    add(new Label("Time Does Not Match Comment:"), 0, 2)
    add(timeDoesNotMatchCommentInput, 1, 2)
  }

  private val okButtonType = new ButtonType("Ok", ButtonData.OKDone)

  def showDialog(stage: Stage): Option[DialogResult] =
    val dialog = new Dialog[DialogResult]() {
      initOwner(stage)
      title = "Login Dialog"
      headerText = "Look, a Custom Login Dialog"
      dialogPane = new DialogPane {
        content = grid
        buttonTypes = Seq(okButtonType, ButtonType.Cancel)
      }
    }
    dialog.resultConverter = btn =>
      if btn == okButtonType then
        DialogResult(
          noSubmissionComment = noSubmissionComment.value,
          timeOutComment = timeOutComment.value,
          timeDoesNotMatchComment = timeDoesNotMatchComment.value,
          gradeManual = false
        )
      else null
    dialog.showAndWait()
    dialog.result.value match
      case null => None
      case value => Some(value)
}
