file://<WORKSPACE>/src/main/scala/Gui.scala
### java.lang.AssertionError: assertion failed: position error, parent span does not contain child span
parent      = new Button(null: <notype>) # -1,
parent span = <2272..4799>,
child       = null # -1,
child span  = [2283..4804..4804]

occurred in the presentation compiler.

action parameters:
offset: 2283
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
import model._
import gui.SubmissionRow
import gui.SubmissionTable

case class Person(name: String) {
  val nameProperty = new StringProperty(this, "", name)
}

object ScalaFXHelloWorld extends JFXApp3 {
  val gradeScope: GradeScope =
    GradeScope(
      courseId = "576725",
      questionId = "28044790",
      assignmentId = "3480775"
    )

  val canvas: Canvas = Canvas(
    courseId = "72193",
    quizId = "103054",
    questionId = "2075376"
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

      val submissionRows = ObservableBuffer[SubmissionRow]()

      val gradeTimeOutChecked = new BooleanProperty()
      val showTimeOutChecked = new BooleanProperty()
      val showTimeNotMatchChecked = new BooleanProperty()

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

      val btnShowDialog = new Button(@@

      val root = new VBox {
        margin = Insets(10)
        padding = Insets(10)
        spacing = 10
        children = Seq(
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
          SubmissionTable(submissionRows).view
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
        val task = for {
          manualGraded <- gradeScope.getManualGradingList
          scores <- gradeScope.getScores
          submissionList <- canvas.getStudentSubmission()
        } yield {
          val manualSet = manualGraded.toSet
          scores.map { (name, gs) =>
            SubmissionRow(
              gs,
              submissionList.get(name),
              if manualSet.contains(name) then Some(true) else Some(false),
              None,
              None
            )
          }
        }
        val result = task
          .fold(
            err =>
              Future {
                onFX {
                  new Alert(AlertType.Information, s"${err}!!!").showAndWait()
                }
              },
            scores =>
              submissionRows.clear()
              submissionRows ++= scores.toList
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

      scene = new Scene(root, 1100, 800)
    }
  }
}

```



#### Error stacktrace:

```
scala.runtime.Scala3RunTime$.assertFailed(Scala3RunTime.scala:8)
	dotty.tools.dotc.ast.Positioned.check$1(Positioned.scala:175)
	dotty.tools.dotc.ast.Positioned.check$1$$anonfun$3(Positioned.scala:205)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	scala.collection.immutable.List.foreach(List.scala:333)
	dotty.tools.dotc.ast.Positioned.check$1(Positioned.scala:205)
	dotty.tools.dotc.ast.Positioned.checkPos(Positioned.scala:226)
	dotty.tools.dotc.ast.Positioned.check$1(Positioned.scala:200)
	dotty.tools.dotc.ast.Positioned.checkPos(Positioned.scala:226)
	dotty.tools.dotc.ast.Positioned.check$1(Positioned.scala:200)
	dotty.tools.dotc.ast.Positioned.check$1$$anonfun$3(Positioned.scala:205)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	scala.collection.immutable.List.foreach(List.scala:333)
	dotty.tools.dotc.ast.Positioned.check$1(Positioned.scala:205)
	dotty.tools.dotc.ast.Positioned.checkPos(Positioned.scala:226)
	dotty.tools.dotc.ast.Positioned.check$1(Positioned.scala:200)
	dotty.tools.dotc.ast.Positioned.checkPos(Positioned.scala:226)
	dotty.tools.dotc.ast.Positioned.check$1(Positioned.scala:200)
	dotty.tools.dotc.ast.Positioned.checkPos(Positioned.scala:226)
	dotty.tools.dotc.ast.Positioned.check$1(Positioned.scala:200)
	dotty.tools.dotc.ast.Positioned.checkPos(Positioned.scala:226)
	dotty.tools.dotc.ast.Positioned.check$1(Positioned.scala:200)
	dotty.tools.dotc.ast.Positioned.checkPos(Positioned.scala:226)
	dotty.tools.dotc.ast.Positioned.check$1(Positioned.scala:200)
	dotty.tools.dotc.ast.Positioned.check$1$$anonfun$3(Positioned.scala:205)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	scala.collection.immutable.List.foreach(List.scala:333)
	dotty.tools.dotc.ast.Positioned.check$1(Positioned.scala:205)
	dotty.tools.dotc.ast.Positioned.checkPos(Positioned.scala:226)
	dotty.tools.dotc.ast.Positioned.check$1(Positioned.scala:200)
	dotty.tools.dotc.ast.Positioned.checkPos(Positioned.scala:226)
	dotty.tools.dotc.ast.Positioned.check$1(Positioned.scala:200)
	dotty.tools.dotc.ast.Positioned.check$1$$anonfun$3(Positioned.scala:205)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	scala.collection.immutable.List.foreach(List.scala:333)
	dotty.tools.dotc.ast.Positioned.check$1(Positioned.scala:205)
	dotty.tools.dotc.ast.Positioned.checkPos(Positioned.scala:226)
	dotty.tools.dotc.parsing.Parser.parse$$anonfun$1(ParserPhase.scala:38)
	dotty.tools.dotc.parsing.Parser.parse$$anonfun$adapted$1(ParserPhase.scala:39)
	scala.Function0.apply$mcV$sp(Function0.scala:42)
	dotty.tools.dotc.core.Phases$Phase.monitor(Phases.scala:440)
	dotty.tools.dotc.parsing.Parser.parse(ParserPhase.scala:39)
	dotty.tools.dotc.parsing.Parser.runOn$$anonfun$1(ParserPhase.scala:48)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	scala.collection.immutable.List.foreach(List.scala:333)
	dotty.tools.dotc.parsing.Parser.runOn(ParserPhase.scala:48)
	dotty.tools.dotc.Run.runPhases$1$$anonfun$1(Run.scala:246)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	scala.collection.ArrayOps$.foreach$extension(ArrayOps.scala:1321)
	dotty.tools.dotc.Run.runPhases$1(Run.scala:262)
	dotty.tools.dotc.Run.compileUnits$$anonfun$1(Run.scala:270)
	dotty.tools.dotc.Run.compileUnits$$anonfun$adapted$1(Run.scala:279)
	dotty.tools.dotc.util.Stats$.maybeMonitored(Stats.scala:67)
	dotty.tools.dotc.Run.compileUnits(Run.scala:279)
	dotty.tools.dotc.Run.compileSources(Run.scala:194)
	dotty.tools.dotc.interactive.InteractiveDriver.run(InteractiveDriver.scala:165)
	scala.meta.internal.pc.MetalsDriver.run(MetalsDriver.scala:45)
	scala.meta.internal.pc.SignatureHelpProvider$.signatureHelp(SignatureHelpProvider.scala:40)
	scala.meta.internal.pc.ScalaPresentationCompiler.signatureHelp$$anonfun$1(ScalaPresentationCompiler.scala:375)
```
#### Short summary: 

java.lang.AssertionError: assertion failed: position error, parent span does not contain child span
parent      = new Button(null: <notype>) # -1,
parent span = <2272..4799>,
child       = null # -1,
child span  = [2283..4804..4804]