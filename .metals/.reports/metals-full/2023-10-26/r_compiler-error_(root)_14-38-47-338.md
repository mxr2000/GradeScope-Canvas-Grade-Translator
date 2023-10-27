file://<WORKSPACE>/src/main/scala/gui/SubmissionTable.scala
### java.lang.AssertionError: NoDenotation.owner

occurred in the presentation compiler.

action parameters:
offset: 1871
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
import scalafx.scene.shape.Rectangle
import scalafx.scene.paint.Color
import scalafx.beans.binding._
import javafx.{collections => jfxc, event => jfxe, scene => jfxs, util => jfxu}

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
  showGradedChecked.onChange { (_ o n@@) =>
    println()
  }
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

  val gradedGroup = new HBox {
    children = Seq(
      new Text("Sort:"),
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

  val shownRows =
    new ObservableBuffer[SubmissionRow](submissionRows)

  val bindings =
    Bindings.createObjectBinding[jfxc.ObservableList[SubmissionRow]](
      () => submissionRows,
      showGradedChecked,
      showNotGradedChecked,
      showTranslatedChecked,
      showNotTranslatedChecked
    )

//   val listBinding = Bindings.createObjectBinding(
//     () => shownRows.source.filter(s => filterRow(s)),
//     showGradedChecked,
//     showNotGradedChecked,
//     showTranslatedChecked,
//     showNotTranslatedChecked
//   )

  val tableView = new TableView[SubmissionRow](shownRows) {
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
      new TableColumn[SubmissionRow, String] {
        text = "Time"
        cellValueFactory = _.value.timeProperty
        prefWidth = 100
      },
      new TableColumn[SubmissionRow, String] {
        text = "GradeScope Link"
        cellValueFactory = _.value.gradeScopeLinkProperty
        prefWidth = 100
        cellFactory = { (_, link) =>
          Hyperlink(s"${link}")
        }
      },
      new TableColumn[SubmissionRow, Option[Boolean]] {
        text = "Manual Graded"
        // cellValueFactory = _.value.gradedProperty
        prefWidth = 100
        cellFactory = { (_, _) =>
          new Rectangle {
            width = 10
            height = 10
            fill = Color.Red
          }
        }
      },
      new TableColumn[SubmissionRow, Option[Boolean]] {
        text = "Translated"
        cellValueFactory = _.value.translatedProperty
        prefWidth = 100
        cellFactory = { (_, link) =>
          Hyperlink(s"https://canvas.its.virginia.edu${link}")
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
        cellFactory = { (_, link) =>
          Hyperlink(s"https://canvas.its.virginia.edu${link}")
        }
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
dotty.tools.dotc.core.SymDenotations$NoDenotation$.owner(SymDenotations.scala:2582)
	scala.meta.internal.pc.SignatureHelpProvider$.isValid(SignatureHelpProvider.scala:83)
	scala.meta.internal.pc.SignatureHelpProvider$.notCurrentApply(SignatureHelpProvider.scala:94)
	scala.meta.internal.pc.SignatureHelpProvider$.$anonfun$1(SignatureHelpProvider.scala:48)
	scala.collection.StrictOptimizedLinearSeqOps.loop$3(LinearSeq.scala:280)
	scala.collection.StrictOptimizedLinearSeqOps.dropWhile(LinearSeq.scala:282)
	scala.collection.StrictOptimizedLinearSeqOps.dropWhile$(LinearSeq.scala:278)
	scala.collection.immutable.List.dropWhile(List.scala:79)
	scala.meta.internal.pc.SignatureHelpProvider$.signatureHelp(SignatureHelpProvider.scala:48)
	scala.meta.internal.pc.ScalaPresentationCompiler.signatureHelp$$anonfun$1(ScalaPresentationCompiler.scala:375)
```
#### Short summary: 

java.lang.AssertionError: NoDenotation.owner