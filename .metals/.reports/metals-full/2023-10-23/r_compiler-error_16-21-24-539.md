file://<WORKSPACE>/src/main/scala/main.scala
### file%3A%2F%2F%2FUsers%2Fmxr%2FDocuments%2Fsrc%2Fscala%2FGradeTranslator%2FGradeTranslator%2Fsrc%2Fmain%2Fscala%2Fmain.scala:18: error: illegal start of definition def
def postGrade(
^

occurred in the presentation compiler.

action parameters:
uri: file://<WORKSPACE>/src/main/scala/main.scala
text:
```scala
import java.time.LocalDateTime
import scala.collection.parallel.CollectionConverters._
import scala.collection.parallel._
import cats.data.State

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
scala.meta.internal.parsers.Reporter.syntaxError(Reporter.scala:16)
	scala.meta.internal.parsers.Reporter.syntaxError$(Reporter.scala:16)
	scala.meta.internal.parsers.Reporter$$anon$1.syntaxError(Reporter.scala:22)
	scala.meta.internal.parsers.Reporter.syntaxError(Reporter.scala:17)
	scala.meta.internal.parsers.Reporter.syntaxError$(Reporter.scala:17)
	scala.meta.internal.parsers.Reporter$$anon$1.syntaxError(Reporter.scala:22)
	scala.meta.internal.parsers.ScalametaParser.statSeqBuf(ScalametaParser.scala:4464)
	scala.meta.internal.parsers.ScalametaParser.$anonfun$batchSource$13(ScalametaParser.scala:4696)
	scala.Option.getOrElse(Option.scala:189)
	scala.meta.internal.parsers.ScalametaParser.$anonfun$batchSource$1(ScalametaParser.scala:4696)
	scala.meta.internal.parsers.ScalametaParser.atPos(ScalametaParser.scala:319)
	scala.meta.internal.parsers.ScalametaParser.autoPos(ScalametaParser.scala:365)
	scala.meta.internal.parsers.ScalametaParser.batchSource(ScalametaParser.scala:4652)
	scala.meta.internal.parsers.ScalametaParser.$anonfun$source$1(ScalametaParser.scala:4645)
	scala.meta.internal.parsers.ScalametaParser.atPos(ScalametaParser.scala:319)
	scala.meta.internal.parsers.ScalametaParser.autoPos(ScalametaParser.scala:365)
	scala.meta.internal.parsers.ScalametaParser.source(ScalametaParser.scala:4645)
	scala.meta.internal.parsers.ScalametaParser.entrypointSource(ScalametaParser.scala:4650)
	scala.meta.internal.parsers.ScalametaParser.parseSourceImpl(ScalametaParser.scala:135)
	scala.meta.internal.parsers.ScalametaParser.$anonfun$parseSource$1(ScalametaParser.scala:132)
	scala.meta.internal.parsers.ScalametaParser.parseRuleAfterBOF(ScalametaParser.scala:59)
	scala.meta.internal.parsers.ScalametaParser.parseRule(ScalametaParser.scala:54)
	scala.meta.internal.parsers.ScalametaParser.parseSource(ScalametaParser.scala:132)
	scala.meta.parsers.Parse$.$anonfun$parseSource$1(Parse.scala:29)
	scala.meta.parsers.Parse$$anon$1.apply(Parse.scala:36)
	scala.meta.parsers.Api$XtensionParseDialectInput.parse(Api.scala:25)
	scala.meta.internal.semanticdb.scalac.ParseOps$XtensionCompilationUnitSource.toSource(ParseOps.scala:17)
	scala.meta.internal.semanticdb.scalac.TextDocumentOps$XtensionCompilationUnitDocument.toTextDocument(TextDocumentOps.scala:206)
	scala.meta.internal.pc.SemanticdbTextDocumentProvider.textDocument(SemanticdbTextDocumentProvider.scala:54)
	scala.meta.internal.pc.ScalaPresentationCompiler.$anonfun$semanticdbTextDocument$1(ScalaPresentationCompiler.scala:356)
```
#### Short summary: 

file%3A%2F%2F%2FUsers%2Fmxr%2FDocuments%2Fsrc%2Fscala%2FGradeTranslator%2FGradeTranslator%2Fsrc%2Fmain%2Fscala%2Fmain.scala:18: error: illegal start of definition def
def postGrade(
^