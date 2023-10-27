file://<WORKSPACE>/src/main/scala/main.scala
### java.lang.AssertionError: assertion failed: position error, parent span does not contain child span
parent      = new File(null: <notype>) # -1,
parent span = <4109..4175>,
child       = null # -1,
child span  = [4118..4177..4177]

occurred in the presentation compiler.

action parameters:
offset: 4118
uri: file://<WORKSPACE>/src/main/scala/main.scala
text:
```scala
import java.time.LocalDateTime
import scala.collection.parallel.CollectionConverters._
import scala.collection.parallel._
import cats.data.State
import Util.SubmissionItem
import java.io.File

case class Report(
    successfulList: List[String] = List(),
    noSubmissionOnCanvasList: List[String] = List(),
    noSubmissionOnGradeScopeList: List[String] = List(),
    timeOutList: List[(String, Int)] = List(),
    submissionDoesNotMatchList: List[(String, LocalDateTime, LocalDateTime)] =
      List(),
    postFailedList: List[(String, String)] = List(),
    submissionMultipleTimesList: List[(String, LocalDateTime, LocalDateTime)] =
      List()
)

def postGrade(
    name: String,
    link: String,
    score: String,
    comment: String = "",
    report: Report,
    onPostSucceed: Report => Report
)(using
    canvas: Canvas
): Report =
  canvas.postQuizGrade(link, score, comment) match
    case Left(err) =>
      println("post failed")
      report.copy(postFailedList = (name, err) :: report.postFailedList)
    case Right(_) =>
      onPostSucceed(report)

def handPostGradeState(
    name: String,
    link: String,
    score: String
): State[Report, Unit] = State.modify { report =>
  report.copy(successfulList = name :: report.successfulList)
}

def handlePostGrade(name: String, link: String, score: String, report: Report)(
    using canvas: Canvas
): Report =
  postGrade(
    name,
    link,
    score,
    "",
    report,
    { r =>
      println(s"Post ${name} ${score} success")
      report.copy(successfulList = name :: report.successfulList)
    }
  )

def handleNoSubmissionOnGradeScope(name: String, report: Report)(using
    canvas: Canvas
)(using
    canvasSubmissions: Map[String, CanvasSubmission]
): Report =
  println(s"${name} no submission on grade scope")
  canvasSubmissions
    .get(name)
    .map(cs =>
      postGrade(
        name,
        cs.link,
        "0.0",
        "no submission on grade scope",
        report,
        { r =>
          println("success")
          r.copy(noSubmissionOnGradeScopeList =
            name :: report.noSubmissionOnGradeScopeList
          )

        }
      )
    )
    .getOrElse(
      report.copy(noSubmissionOnCanvasList =
        name :: report.noSubmissionOnCanvasList
      )
    )

def handleSubmissionDoesNotMatch(
    name: String,
    canvasTime: LocalDateTime,
    gradeScopeTime: LocalDateTime,
    report: Report
): Report =
  println(
    s"${name} does not match ${gradeScopeTime} ${canvasTime}"
  )
  report.copy(submissionDoesNotMatchList =
    (
      name,
      gradeScopeTime,
      canvasTime
    ) :: report.submissionDoesNotMatchList
  )

def handleNoSubmissionOnCanvas(
    name: String,
    report: Report
) =
  println(s"${name} no submission on canvas")
  report.copy(noSubmissionOnCanvasList =
    name :: report.noSubmissionOnCanvasList
  )

def handleSubmissionTimeOut(
    name: String,
    link: String,
    duration: Int,
    report: Report
)(using
    canvas: Canvas
) =
  postGrade(
    name,
    link,
    "0.0",
    "timeout",
    report,
    { r =>
      println(s"${name} time out: ${duration}")
      report.copy(timeOutList = (name, duration) :: report.timeOutList)
    }
  )

def handleMultipleSubmission(
    name: String,
    link: String,
    score: String,
    gradeScopeTime: LocalDateTime,
    canvasTime: LocalDateTime,
    report: Report
)(using
    canvas: Canvas
) =
  postGrade(
    name,
    link,
    score,
    "",
    report,
    { r =>
      println(s"${name} submitted twice")
      r.copy(submissionMultipleTimesList =
        (
          name,
          gradeScopeTime,
          canvasTime
        ) :: report.submissionMultipleTimesList
      )
    }
  )

enum ActivationResult(name: String):
  case Activated(name: String, item: SubmissionItem)
      extends ActivationResult(name)
  case NoItemFound(name: String) extends ActivationResult(name)
  case Error(name: String, error: String) extends ActivationResult(name)

def writeToCsv(results: List[ActivationResult]) =
  import com.github.tototoshi.csv.CSVWriter
  val csvFile = "activation.csv"

  val file = new File(@@
  val csvWriter = CSVWriter.open(fileWriter)
  println()

def activateSubmissions(using canvas: Canvas)(using
    gradeScope: GradeScope
): Unit =
  val result = for {
    submissionList <- canvas.getStudentSubmission()
    scores <- gradeScope.getScores
  } yield submissionList.foldLeft[List[ActivationResult]](List()) { (acc, kv) =>
    val (name, cs) = kv
    scores.get(name) match {
      case None => acc
      case Some(gs) =>
        canvas.getSubmissionStatus(cs.link, gs.time) match
          case SubmissionStatus.SubmissionDoesNotMatch(
                gradeScopeTime,
                canvasTime
              ) =>
            gradeScope.getSubmissionItems(gs.link) match
              case Left(err) => ActivationResult.Error(name, err) :: acc
              case Right(items) =>
                Util.findLatestBefore(items, canvasTime) match
                  case None => ActivationResult.NoItemFound(name) :: acc
                  case Some(item) =>
                    gradeScope.activateSubmission(item) match
                      case Left(err) => ActivationResult.Error(name, err) :: acc
                      case Right(value) =>
                        ActivationResult.Activated(name, item) :: acc
          case _ => acc
    }
  }
  result match
    case Left(err) => println(err)
    case Right(results) =>
      println()

  println("")

@main
def main(): Unit = {
  given canvas: Canvas = Canvas(
    courseId = "72193",
    quizId = "103054"
  )

  given gradeScope: GradeScope =
    GradeScope(
      courseId = "576725",
      questionId = "28044790",
      assignmentId = "3480775"
    )

  val report = for {
    manualList <- gradeScope.getManualGradingList
    scores <- gradeScope.getScores
    submissionList <- canvas.getStudentSubmission()
  } yield manualList.par.foldLeft(Report()) { (report, name) =>
    given canvasSubmissions: Map[String, CanvasSubmission] = submissionList

    scores.get(name) match
      case None =>
        handleNoSubmissionOnGradeScope(name, report)
      case Some(gs) =>
        submissionList.get(name) match
          case None =>
            handleNoSubmissionOnCanvas(name, report)
          case Some(cs) =>
            canvas.getSubmissionStatus(cs.link, gs.time) match
              case SubmissionStatus.SubmissionTimeOut(duration) =>
                handleSubmissionTimeOut(name, cs.link, duration, report)
              case SubmissionStatus.SubmissionDoesNotSubmitOnCanvas =>
                handleNoSubmissionOnCanvas(name, report)
              case SubmissionStatus.SubmissionMultipleTimes(
                    gradeScopeTime,
                    canvasTime
                  ) =>
                handleMultipleSubmission(
                  name,
                  cs.link,
                  gs.score,
                  gradeScopeTime,
                  canvasTime,
                  report
                )
              case SubmissionStatus.SubmissionDoesNotMatch(
                    gradeScopeTime,
                    canvasTime
                  ) =>
                handleSubmissionDoesNotMatch(
                  name,
                  canvasTime,
                  gradeScopeTime,
                  report
                )
              case SubmissionStatus.SubmissionSuccess =>
                handlePostGrade(name, cs.link, gs.score, report)

  }
  report match
    case Left(err) => println(s"${err}")
    case Right(report: Report) =>
      println(
        s"Successful: ${report.successfulList.size} + ${report.submissionMultipleTimesList.size}"
      )
      println(
        s"No Submission on GradeScope: ${report.noSubmissionOnGradeScopeList}"
      )
      println(s"No Submission on Canvas: ${report.noSubmissionOnCanvasList}")
      println(
        s"Submission Time Does Not Match: ${report.submissionDoesNotMatchList}"
      )
      println(s"Submission Time Out: ${report.timeOutList}")
      println(s"Submission twice: ${report.submissionMultipleTimesList}")
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
parent      = new File(null: <notype>) # -1,
parent span = <4109..4175>,
child       = null # -1,
child span  = [4118..4177..4177]