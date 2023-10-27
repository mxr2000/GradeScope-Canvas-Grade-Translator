file://<WORKSPACE>/src/main/scala/Gui.scala
### java.nio.file.NoSuchFileException: <WORKSPACE>/.bloop/root/bloop-bsp-clients-classes/classes-Metals-GoD1ZFQeTiW6npjPr2e7iw==/main$package.class

occurred in the presentation compiler.

action parameters:
offset: 1407
uri: file://<WORKSPACE>/src/main/scala/Gui.scala
text:
```scala
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

case class Person(name: String) {
  val nameProperty = new StringProperty(this, "", name)
}

implicit class GradeScopeSubmissionExtensions(
    submission: GradeScopeSubmission
) {
  def nameProperty = new StringProperty(submission, "name", submission.name)
  def scoreProperty = new StringProperty(submission, "score", submission.score)
  def timeProperty =
    new StringProperty(submission, "time", submission.time.toString())
  def linkProperty = new StringProperty(submission, "link", submission.link)
  d@@
}



object ScalaFXHelloWorld extends JFXApp3 {
  val gradeScope: GradeScope =
    GradeScope(
      courseId = "576725",
      questionId = "28044790",
      assignmentId = "3480775"
    )

  override def start(): Unit = {
    stage = new JFXApp3.PrimaryStage {
      title = "ToDo List"

      val toDoItems = ObservableBuffer[String]()
      val ip =
        new ObjectProperty[jfxsc.MultipleSelectionModel[String]]()
      val sp = new StringProperty("")

      val people = ObservableBuffer[GradeScopeSubmission](
      )

      val gradeTimeOutChecked = new BooleanProperty()
      val showTimeOutChecked = new BooleanProperty()
      val showTimeNotMatchChecked = new BooleanProperty()
      val kk = Bindings.createObjectBinding(
        () => if gradeTimeOutChecked.value then toDoItems else toDoItems,
        gradeTimeOutChecked,
        toDoItems
      )

      val cb = new CheckBox("") {
        gradeTimeOutChecked <== selected
      }

      val cb1 = new CheckBox("Show time out") {
        showTimeOutChecked <== selected
      }

      val cb2 = new CheckBox("Show Not match") {
        showTimeNotMatchChecked <== selected
      }

      val rb1 = new RadioButton("time") {}

      val tv = new TableView[GradeScopeSubmission](people) {
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
            text = "Link"
            cellValueFactory = _.value.linkProperty
            prefWidth = 100
          }
        )
      }

      val root = new VBox {
        margin = Insets(10)
        padding = Insets(10)
        spacing = 10
        children = Seq(
          new ListView[String]() {
            items = toDoItems
            ip <== selectionModel
          },
          new HBox {
            children = Seq(
              new TextField() {
                promptText = "Add a new task"
                spacing = 10
                text <==> sp
              },
              new Button("Add") {
                onAction = () => addItem()
              },
              new Button("Remove") {
                onAction = () => removeItem()
              },
              new Button("load") {
                onAction = () => loadGradeScopeScores()
              },
              cb
            )
          },
          tv
        )
      }

      def addItem(): Unit = {
        val task = sp.getValue().trim
        if (task.nonEmpty) {
          toDoItems.add(task)
          sp.setValue("")
        }
      }

      def removeItem(): Unit = {
        val selectedItemIndex =
          ip.getValue().getSelectedIndex()

        if (selectedItemIndex >= 0) {
          toDoItems.remove(selectedItemIndex)
        }
      }

      def loadGradeScopeScores(): Unit = {
        import org.scalafx.extras._
        val task = gradeScope.getScores
        val result = task
          .fold(
            err =>
              Future {
                onFX {
                  new Alert(AlertType.Information, s"${err}!!!").showAndWait()
                }
              },
            scores =>
              people.clear()
              people ++= scores.map((_, s) => s).toList
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

      scene = new Scene(root, 800, 400)
    }
  }
}

```



#### Error stacktrace:

```
java.base/sun.nio.fs.UnixException.translateToIOException(UnixException.java:92)
	java.base/sun.nio.fs.UnixException.rethrowAsIOException(UnixException.java:106)
	java.base/sun.nio.fs.UnixException.rethrowAsIOException(UnixException.java:111)
	java.base/sun.nio.fs.UnixFileAttributeViews$Basic.readAttributes(UnixFileAttributeViews.java:55)
	java.base/sun.nio.fs.UnixFileSystemProvider.readAttributes(UnixFileSystemProvider.java:148)
	java.base/java.nio.file.Files.readAttributes(Files.java:1851)
	java.base/java.nio.file.Files.getLastModifiedTime(Files.java:2402)
	dotty.tools.io.Path.lastModified(Path.scala:196)
	dotty.tools.io.PlainFile.lastModified(PlainFile.scala:84)
	dotty.tools.dotc.core.SymDenotations$PackageClassDenotation.$anonfun$14(SymDenotations.scala:2506)
	scala.collection.StrictOptimizedIterableOps.map(StrictOptimizedIterableOps.scala:100)
	scala.collection.StrictOptimizedIterableOps.map$(StrictOptimizedIterableOps.scala:87)
	scala.collection.immutable.Set$Set2.map(Set.scala:183)
	dotty.tools.dotc.core.SymDenotations$PackageClassDenotation.dropStale$1(SymDenotations.scala:2506)
	dotty.tools.dotc.core.SymDenotations$PackageClassDenotation.recur$5(SymDenotations.scala:2480)
	dotty.tools.dotc.core.SymDenotations$PackageClassDenotation.computeMembersNamed(SymDenotations.scala:2539)
	dotty.tools.dotc.core.SymDenotations$ClassDenotation.membersNamed(SymDenotations.scala:2090)
	dotty.tools.dotc.core.SymDenotations$ClassDenotation.findMember(SymDenotations.scala:2141)
	dotty.tools.dotc.core.Types$Type.go$1(Types.scala:695)
	dotty.tools.dotc.core.Types$Type.goThis$1(Types.scala:801)
	dotty.tools.dotc.core.Types$Type.go$1(Types.scala:712)
	dotty.tools.dotc.core.Types$Type.findMember(Types.scala:874)
	dotty.tools.dotc.core.Types$Type.memberBasedOnFlags(Types.scala:678)
	dotty.tools.dotc.core.Types$Type.member(Types.scala:662)
	dotty.tools.dotc.core.Types$Type.allMembers$$anonfun$1(Types.scala:1015)
	scala.runtime.function.JProcedure2.apply(JProcedure2.java:15)
	scala.runtime.function.JProcedure2.apply(JProcedure2.java:10)
	dotty.tools.dotc.core.Types$Type.memberDenots$$anonfun$1(Types.scala:920)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	scala.collection.immutable.BitmapIndexedSetNode.foreach(HashSet.scala:937)
	scala.collection.immutable.HashSet.foreach(HashSet.scala:944)
	dotty.tools.dotc.core.Types$Type.memberDenots(Types.scala:920)
	dotty.tools.dotc.core.Types$Type.allMembers(Types.scala:1015)
	scala.meta.internal.pc.IndexedContext$.accesibleMembers$1(IndexedContext.scala:154)
	scala.meta.internal.pc.IndexedContext$.scala$meta$internal$pc$IndexedContext$$$extractNames(IndexedContext.scala:216)
	scala.meta.internal.pc.IndexedContext$LazyWrapper.<init>(IndexedContext.scala:100)
	scala.meta.internal.pc.IndexedContext$.apply(IndexedContext.scala:88)
	scala.meta.internal.pc.IndexedContext$LazyWrapper.<init>(IndexedContext.scala:99)
	scala.meta.internal.pc.IndexedContext$.apply(IndexedContext.scala:88)
	scala.meta.internal.pc.IndexedContext$LazyWrapper.<init>(IndexedContext.scala:99)
	scala.meta.internal.pc.IndexedContext$.apply(IndexedContext.scala:88)
	scala.meta.internal.pc.IndexedContext$LazyWrapper.<init>(IndexedContext.scala:99)
	scala.meta.internal.pc.IndexedContext$.apply(IndexedContext.scala:88)
	scala.meta.internal.pc.IndexedContext$LazyWrapper.<init>(IndexedContext.scala:99)
	scala.meta.internal.pc.IndexedContext$.apply(IndexedContext.scala:88)
	scala.meta.internal.pc.IndexedContext$LazyWrapper.<init>(IndexedContext.scala:99)
	scala.meta.internal.pc.IndexedContext$.apply(IndexedContext.scala:88)
	scala.meta.internal.pc.IndexedContext$LazyWrapper.<init>(IndexedContext.scala:99)
	scala.meta.internal.pc.IndexedContext$.apply(IndexedContext.scala:88)
	scala.meta.internal.pc.IndexedContext$LazyWrapper.<init>(IndexedContext.scala:99)
	scala.meta.internal.pc.IndexedContext$.apply(IndexedContext.scala:88)
	scala.meta.internal.pc.IndexedContext$LazyWrapper.<init>(IndexedContext.scala:99)
	scala.meta.internal.pc.IndexedContext$.apply(IndexedContext.scala:88)
	scala.meta.internal.pc.IndexedContext$LazyWrapper.<init>(IndexedContext.scala:99)
	scala.meta.internal.pc.IndexedContext$.apply(IndexedContext.scala:88)
	scala.meta.internal.pc.IndexedContext$LazyWrapper.<init>(IndexedContext.scala:99)
	scala.meta.internal.pc.IndexedContext$.apply(IndexedContext.scala:88)
	scala.meta.internal.pc.IndexedContext$LazyWrapper.<init>(IndexedContext.scala:99)
	scala.meta.internal.pc.IndexedContext$.apply(IndexedContext.scala:88)
	scala.meta.internal.pc.IndexedContext$LazyWrapper.<init>(IndexedContext.scala:99)
	scala.meta.internal.pc.IndexedContext$.apply(IndexedContext.scala:88)
	scala.meta.internal.pc.IndexedContext$LazyWrapper.<init>(IndexedContext.scala:99)
	scala.meta.internal.pc.IndexedContext$.apply(IndexedContext.scala:88)
	scala.meta.internal.pc.IndexedContext$LazyWrapper.<init>(IndexedContext.scala:99)
	scala.meta.internal.pc.IndexedContext$.apply(IndexedContext.scala:88)
	scala.meta.internal.pc.IndexedContext$LazyWrapper.<init>(IndexedContext.scala:99)
	scala.meta.internal.pc.IndexedContext$.apply(IndexedContext.scala:88)
	scala.meta.internal.pc.IndexedContext$LazyWrapper.<init>(IndexedContext.scala:99)
	scala.meta.internal.pc.IndexedContext$.apply(IndexedContext.scala:88)
	scala.meta.internal.pc.IndexedContext$LazyWrapper.<init>(IndexedContext.scala:99)
	scala.meta.internal.pc.IndexedContext$.apply(IndexedContext.scala:88)
	scala.meta.internal.pc.IndexedContext$LazyWrapper.<init>(IndexedContext.scala:99)
	scala.meta.internal.pc.IndexedContext$.apply(IndexedContext.scala:88)
	scala.meta.internal.pc.IndexedContext$LazyWrapper.<init>(IndexedContext.scala:99)
	scala.meta.internal.pc.IndexedContext$.apply(IndexedContext.scala:88)
	scala.meta.internal.pc.IndexedContext$LazyWrapper.<init>(IndexedContext.scala:99)
	scala.meta.internal.pc.IndexedContext$.apply(IndexedContext.scala:88)
	scala.meta.internal.pc.IndexedContext$LazyWrapper.<init>(IndexedContext.scala:99)
	scala.meta.internal.pc.IndexedContext$.apply(IndexedContext.scala:88)
	scala.meta.internal.pc.IndexedContext$LazyWrapper.<init>(IndexedContext.scala:99)
	scala.meta.internal.pc.IndexedContext$.apply(IndexedContext.scala:88)
	scala.meta.internal.pc.IndexedContext$LazyWrapper.<init>(IndexedContext.scala:99)
	scala.meta.internal.pc.IndexedContext$.apply(IndexedContext.scala:88)
	scala.meta.internal.pc.IndexedContext$LazyWrapper.<init>(IndexedContext.scala:99)
	scala.meta.internal.pc.IndexedContext$.apply(IndexedContext.scala:88)
	scala.meta.internal.pc.IndexedContext$LazyWrapper.<init>(IndexedContext.scala:99)
	scala.meta.internal.pc.IndexedContext$.apply(IndexedContext.scala:88)
	scala.meta.internal.pc.IndexedContext$LazyWrapper.<init>(IndexedContext.scala:99)
	scala.meta.internal.pc.IndexedContext$.apply(IndexedContext.scala:88)
	scala.meta.internal.pc.IndexedContext$LazyWrapper.<init>(IndexedContext.scala:99)
	scala.meta.internal.pc.IndexedContext$.apply(IndexedContext.scala:88)
	scala.meta.internal.pc.IndexedContext$LazyWrapper.<init>(IndexedContext.scala:99)
	scala.meta.internal.pc.IndexedContext$.apply(IndexedContext.scala:88)
	scala.meta.internal.pc.IndexedContext$LazyWrapper.<init>(IndexedContext.scala:99)
	scala.meta.internal.pc.IndexedContext$.apply(IndexedContext.scala:88)
	scala.meta.internal.pc.IndexedContext$LazyWrapper.<init>(IndexedContext.scala:99)
	scala.meta.internal.pc.IndexedContext$.apply(IndexedContext.scala:88)
	scala.meta.internal.pc.completions.CompletionProvider.completions(CompletionProvider.scala:62)
	scala.meta.internal.pc.ScalaPresentationCompiler.complete$$anonfun$1(ScalaPresentationCompiler.scala:123)
```
#### Short summary: 

java.nio.file.NoSuchFileException: <WORKSPACE>/.bloop/root/bloop-bsp-clients-classes/classes-Metals-GoD1ZFQeTiW6npjPr2e7iw==/main$package.class