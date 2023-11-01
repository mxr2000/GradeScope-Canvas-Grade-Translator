package gui

import model.{CanvasSettings, GradeScopeSettings, Settings, SettingsDao}
import scalafx.scene.layout.{BorderPane, ColumnConstraints, GridPane, Priority}
import scalafx.scene.control.*
import scalafx.stage.Stage
import scalafx.scene.control.ButtonBar.ButtonData
import scalafx.application.JFXApp3
import scalafx.beans.property.StringProperty
import scalafx.geometry.Insets
import scalafx.scene.Scene

object SettingsDialog {
  val canvasCourseId = StringProperty("")
  val canvasQuizId = StringProperty("")
  val canvasQuestionId = StringProperty("")
  val canvasToken = StringProperty("")
  val canvasCookies = StringProperty("")
  val gradeScopeAssignmentId = StringProperty("")
  val gradeScopeCourseId = StringProperty("")
  val gradeScopeQuestionId = StringProperty("")
  val gradeScopeToken = StringProperty("")
  val gradeScopeCookies = StringProperty("")

  val grid: GridPane = new GridPane() {
    margin = Insets(10)
    padding = Insets(10)
    hgap = 10
    vgap = 10
    hgrow = Priority.Always
    vgrow = Priority.Always

    columnConstraints = List(
      new ColumnConstraints {
        percentWidth = 30
      },
      new ColumnConstraints {
        percentWidth = 70
      }
    )

    add(Label("Canvas Course Id"), 0, 0)
    add(Label("Canvas Quiz Id"), 0, 1)
    add(Label("Canvas Question Id"), 0, 2)
    add(Label("GradeScope Course Id"), 0, 3)
    add(Label("GradeScope Assignment Id"), 0, 4)
    add(Label("GradeScope Question Id"), 0, 5)
    add(Label("Canvas Token"), 0, 6)
    add(Label("Canvas Cookies"), 0, 7)
    add(Label("GradeScope Token"), 0, 8)
    add(Label("GradeScope Cookies"), 0, 9)

    add(
      new TextField {
        maxWidth = 100
        text <==> canvasCourseId
      },
      1,
      0
    )
    add(
      new TextField {
        maxWidth = 100
        text <==> canvasQuizId
      },
      1,
      1
    )
    add(
      new TextField {
        maxWidth = 100
        text <==> canvasQuestionId
      },
      1,
      2
    )
    add(
      new TextField {
        maxWidth = 100
        text <==> gradeScopeCourseId
      },
      1,
      3
    )
    add(
      new TextField {
        maxWidth = 100
        text <==> gradeScopeAssignmentId
      },
      1,
      4
    )
    add(
      new TextField {
        maxWidth = 100
        text <==> gradeScopeQuestionId
      },
      1,
      5
    )
    add(
      new TextArea {
        prefHeight = 50
        wrapText = true
        text <==> canvasToken
      },
      1,
      6
    )
    add(
      new TextArea {
        prefHeight = 100
        wrapText = true
        text <==> canvasCookies
      },
      1,
      7
    )
    add(
      new TextArea {
        prefHeight = 50
        wrapText = true
        text <==> gradeScopeToken
      },
      1,
      8
    )
    add(
      new TextArea {
        prefHeight = 100
        wrapText = true
        text <==> gradeScopeCookies
      },
      1,
      9
    )
  }

  val borderPane: BorderPane = new BorderPane {
    center = grid
    style = "-fx-border-color: black;" + "-fx-border-width: 2px;"
  }

  private val saveButtonType = new ButtonType("Save", ButtonData.OKDone)

  val load = {
    SettingsDao.loadSettings("a.json").map { settings =>
      val canvas = settings.canvasSettings
      val gradeScope = settings.gradeScopeSettings
      canvasCookies.value = canvas.cookies
      canvasToken.value = canvas.token
      canvasQuizId.value = canvas.quizId
      canvasQuestionId.value = canvas.questionId
      canvasCourseId.value = canvas.courseId

      gradeScopeCourseId.value = gradeScope.courseId
      gradeScopeQuestionId.value = gradeScope.questionId
      gradeScopeAssignmentId.value = gradeScope.assignmentId
      gradeScopeToken.value = gradeScope.token
      gradeScopeCookies.value = gradeScope.cookies
    }
  }

  private def save = {
    SettingsDao.saveSettings(
      "a.json",
      Settings(
        CanvasSettings(
          canvasCourseId.value,
          canvasQuizId.value,
          canvasQuestionId.value,
          canvasCookies.value,
          canvasToken.value
        ),
        GradeScopeSettings(
          gradeScopeCourseId.value,
          gradeScopeAssignmentId.value,
          gradeScopeQuestionId.value,
          gradeScopeCookies.value,
          gradeScopeToken.value
        )
      )
    )
  }

  def showDialog(stage: Stage) = {
    val dialog = new Dialog[String]() {
      initOwner(stage)
      title = "Settings Dialog"
      dialogPane = new DialogPane {
        content = grid
        buttonTypes = Seq(saveButtonType, ButtonType.Cancel)
      }
    }
    dialog.resultConverter = btn =>
      if btn == saveButtonType then
        save
        "Saved"
      else null
    load
    dialog.showAndWait()
  }

}

object SettingsDialogApp extends JFXApp3 {
  def start(): Unit = {
    stage = new JFXApp3.PrimaryStage {
      scene = new Scene(SettingsDialog.borderPane, 600, 700)
    }
  }
}
