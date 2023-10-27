file://<WORKSPACE>/src/main/scala/gui/SubmissionTable.scala
### java.lang.StringIndexOutOfBoundsException: String index out of range: 0

occurred in the presentation compiler.

action parameters:
offset: 4794
uri: file://<WORKSPACE>/src/main/scala/gui/SubmissionTable.scala
text:
```scala
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

implicit class GradeScopeSubmissionExtensions(
    submission: GradeScopeSubmission
)

implicit class CanvasSubmissionExtensions(
    submission: CanvasSubmission
) {
  def nameProperty = StringProperty(submission.name)
  def linkProperty = StringProperty(submission.link)
}

case class SubmissionRow(
    gs: GradeScopeSubmission,
    cs: Option[CanvasSubmission],
    graded: Option[Boolean],
    translated: Option[Boolean],
    status: Option[SubmissionStatus]
) {
  def nameProperty = StringProperty(gs.name)
  def scoreProperty = StringProperty(gs.score)
  def timeProperty = StringProperty(gs.time.toString())
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

  val sortByTimeSelected = new BooleanProperty()
  val sortByNameSelected = new BooleanProperty()
  val sortByScoreSelected = new BooleanProperty()

  val ip = new ObjectProperty[jfxsc.Toggle]()
  val sortTog = new ToggleGroup {
    ip <== selectedToggle
  }
//   ip.getValue().getUserData() match {
//     case null                 => println()
//     case SortStrategy.ByName  => println()
//     case SortStrategy.ByScore => println()
//     case SortStrategy.ByTime  => println()
//   }
  val translatedGroup = new HBox {
    children = Seq(
      new Text("Sort:"),
      new CheckBox {
        text = "Show Translated"
        showTranslatedChecked <==> selected
      },
      new CheckBox {
        text = "Show Not Translated"
        showNotGradedChecked <==> selected
      }
    )
    padding = Insets(10)
    spacing = 10
  }

  val gradedGroup = new HBox {
    children = Seq(
      new Text("Sort:"),
      new CheckBox {
        text = "Show Graded"
        showGradedChecked <==> selected
      },
      new CheckBox {
        text = "Show Not Graded"
        showNotGradedChecked <==> selected
      }
    )
    padding = Insets(10)
    spacing = 10
  }

  val sortGroups = new HBox {
    children = Seq(
      new Text("Sort:"),
      new RadioButton {
        text = "time"
        toggleGroup = sortTog
        userData = SortStrategy.ByName
        sortByTimeSelected <== selected
      },
      new RadioButton {
        text = "score"
        toggleGroup = sortTog
        userData = SortStrategy.ByName
        sortByScoreSelected <== selected
      },
      new RadioButton {
        text = "name"
        toggleGroup = sortTog
        userData = SortStrategy.ByName
        sortByNameSelected <== selected
      }
    )
    padding = Insets(10)
    spacing = 10
  }

  def filterRow(s: SubmissionRow) =
    (showGradedChecked.value && s.graded == Some(true) ||
      showNotGradedChecked.value && s.graded != Some(true)) &&
      (showTranslatedChecked.value && s.translated == Some(true) ||
        showNotTranslatedChecked.value && s.translated != Some(true))

  val shownRows = new FilteredBuffer[SubmissionRow](submissionRows, filterRow)

  val tableView = new TableView[SubmissionRow](shownRows) {
    vgrow = Priority.ALWAYS

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
      new TableColumn[SubmissionRow, String] {
        text = "Time"
        cellValueFactory = _.value.timeProperty
        prefWidth = 100
      },
      new TableColumn[SubmissionRow, String] {
        text = "GradeScope Link"
        cellValueFactory = _.value.gradeScopeLinkProperty
        prefWidth = 100
        cellFactory = { (_, b) =>
          Hyperlink(s"https://canvas.its.virginia.edu$@@")
        }
      },
      new TableColumn[SubmissionRow, Option[Boolean]] {
        text = "Manual Graded"
        cellValueFactory = _.value.gradedProperty
        prefWidth = 100
        cellFactory = { (_, _) =>
          Hyperlink("")
        }
      },
      new TableColumn[SubmissionRow, Option[Boolean]] {
        text = "Translated"
        cellValueFactory = _.value.translatedProperty
        prefWidth = 100
        cellFactory = { (_, _) =>
          Hyperlink("")
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
      }
    )
  }

  val view = new VBox {
    children = Seq(
      translatedGroup,
      gradedGroup,
      sortGroups,
      tableView
    )
    vgrow = Priority.ALWAYS
  }
}

```



#### Error stacktrace:

```
java.base/java.lang.StringLatin1.charAt(StringLatin1.java:48)
	java.base/java.lang.String.charAt(String.java:1513)
	scala.collection.StringOps$.apply$extension(StringOps.scala:188)
	dotty.tools.dotc.interactive.Completion$.needsBacktick(Completion.scala:187)
	dotty.tools.dotc.interactive.Completion$.backtickCompletions(Completion.scala:167)
	dotty.tools.dotc.interactive.Completion$.$anonfun$1(Completion.scala:154)
	scala.collection.immutable.List.map(List.scala:250)
	dotty.tools.dotc.interactive.Completion$.computeCompletions(Completion.scala:154)
	dotty.tools.dotc.interactive.Completion$.completions(Completion.scala:50)
	scala.meta.internal.pc.completions.Completions.completions(Completions.scala:202)
	scala.meta.internal.pc.completions.CompletionProvider.completions(CompletionProvider.scala:86)
	scala.meta.internal.pc.ScalaPresentationCompiler.complete$$anonfun$1(ScalaPresentationCompiler.scala:123)
```
#### Short summary: 

java.lang.StringIndexOutOfBoundsException: String index out of range: 0