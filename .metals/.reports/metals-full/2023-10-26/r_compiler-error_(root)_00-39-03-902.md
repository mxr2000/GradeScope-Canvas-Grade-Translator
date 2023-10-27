file://<WORKSPACE>/src/main/scala/gui/SubmissionTable.scala
### java.lang.AssertionError: NoDenotation.owner

occurred in the presentation compiler.

action parameters:
offset: 1350
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

final case class SubmissionTable(
    submissionRows: ObservableBuffer[SubmissionRow]
) {
    val i[@@]
  val sortTog = new ToggleGroup {
    selectedToggle
  }

  val translatedGroup = new HBox {
    children = Seq(
      new Text("Sort:"),
      new CheckBox {
        text = "Dick"
      },
      new CheckBox {
        text = "Dick"
      }
    )
    padding = Insets(10)
    spacing = 10
  }

  val gradedGroup = new HBox {
    children = Seq(
      new Text("Sort:"),
      new CheckBox {
        text = "Dick"
      },
      new CheckBox {
        text = "Dick"
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
      },
      new RadioButton {
        text = "score"
        toggleGroup = sortTog
      },
      new RadioButton {
        text = "name"
        toggleGroup = sortTog
      }
    )
    padding = Insets(10)
    spacing = 10
  }

  val tableView = new TableView[SubmissionRow](submissionRows) {
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
          Hyperlink("")
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
dotty.tools.dotc.core.SymDenotations$NoDenotation$.owner(SymDenotations.scala:2582)
	scala.meta.internal.pc.SignatureHelpProvider$.isValid(SignatureHelpProvider.scala:83)
	scala.meta.internal.pc.SignatureHelpProvider$.notCurrentApply(SignatureHelpProvider.scala:96)
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