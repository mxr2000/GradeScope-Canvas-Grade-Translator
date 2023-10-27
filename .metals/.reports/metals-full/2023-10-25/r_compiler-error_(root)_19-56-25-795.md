file://<WORKSPACE>/src/main/scala/gui/SubmissionTable.scala
### java.lang.AssertionError: NoDenotation.owner

occurred in the presentation compiler.

action parameters:
offset: 999
uri: file://<WORKSPACE>/src/main/scala/gui/SubmissionTable.scala
text:
```scala
package gui

import model._
import scalafx.beans.property._
import scalafx.scene.control._

implicit class GradeScopeSubmissionExtensions(
    submission: GradeScopeSubmission
) {
  def nameProperty = new StringProperty(submission, "name", submission.name)
  def scoreProperty = new StringProperty(submission, "score", submission.score)
  def timeProperty =
    new StringProperty(submission, "time", submission.time.toString())
  def linkProperty = new StringProperty(submission, "link", submission.link)
}

implicit class CanvasSubmissionExtensions(
    submission: CanvasSubmission
) {
  def nameProperty = new StringProperty(submission, "name", submission.name)
  def linkProperty = new StringProperty(submission, "link", submission.link)
}

case class TableRow(
    gs: GradeScopeSubmission,
    cs: Option[CanvasSubmission],
    graded: Option[Boolean],
    translated: Option[Boolean],
    status: Option[SubmissionStatus]
)

final case class SubmissionTable(submissionRows: ObservableBuffer[@@]) {

  val tableView = new TableView[GradeScopeSubmission](people) {
    columns ++= List(
      new TableColumn[GradeScopeSubmission, String] {
        text = "Name"
        cellValueFactory = _.value.nameProperty
        prefWidth = 100
      },
      new TableColumn[GradeScopeSubmission, String] {
        text = "Score"
        cellValueFactory = _.value.scoreProperty
        prefWidth = 50
      },
      new TableColumn[GradeScopeSubmission, String] {
        text = "Time"
        cellValueFactory = _.value.timeProperty
        prefWidth = 100
      },
      new TableColumn[GradeScopeSubmission, String] {
        text = "GradeScope Link"
        cellValueFactory = _.value.linkProperty
        prefWidth = 100
      },
      new TableColumn[GradeScopeSubmission, String] {
        text = "Manual Graded"
        cellValueFactory = _.value.linkProperty
        prefWidth = 100
      },
      new TableColumn[GradeScopeSubmission, String] {
        text = "Translated"
        cellValueFactory = _.value.linkProperty
        prefWidth = 100
      },
      new TableColumn[GradeScopeSubmission, String] {
        text = "Status"
        cellValueFactory = _.value.linkProperty
        prefWidth = 100
      },
      new TableColumn[GradeScopeSubmission, String] {
        text = "Canvas Link"
        cellValueFactory = _.value.linkProperty
        prefWidth = 100
      }
    )
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