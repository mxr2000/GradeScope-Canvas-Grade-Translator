import java.time.LocalDateTime
import scala.collection.parallel.CollectionConverters._
import scala.collection.parallel._
import cats.data.State
import model.Util._
import model._
import java.io.File
import java.time.temporal.ChronoUnit
import concurrent.ExecutionContext.Implicits.global

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
  import com.github.tototoshi.csv._
  val csvFile = "activation.csv"

  val file = new File(csvFile)
  val writer = CSVWriter.open(file)

  writer.writeRow(List("Name", "Result"))
  results.foreach(r =>
    val output = r match
      case ActivationResult.Activated(name, item) =>
        List(name, s"${item.score} - ${item.created_at.toString()}")
      case ActivationResult.Error(name, error) => List(name, error)
      case ActivationResult.NoItemFound(name)  => List(name, "not found")
    writer.writeRow(output)
  )
  writer.close()

/* def activateSubmissions(using canvas: Canvas)(using
    gradeScope: GradeScope
): Unit =
  val result = for {
    submissionList <- canvas.getStudentSubmission()
    scores <- gradeScope.getScores
  } yield submissionList.foldLeft[List[ActivationResult]](List()) { (acc, kv) =>
    val (name, cs) = kv
    scores
      .get(name)
      .map { gs =>
        canvas.getSubmissionStatus(cs.link, gs.time) match
          case SubmissionStatus.SubmissionDoesNotMatch(
                gradeScopeTime,
                canvasTime
              ) =>
            val status = for {
              items <- gradeScope.getSubmissionItems(gs.link)
              status <- Util.findLatestBefore(
                items,
                canvasTime.plus(10, ChronoUnit.MINUTES)
              ) match
                case None => Right(ActivationResult.NoItemFound(name))
                case Some(item) =>
                  for {
                    _ <- gradeScope.activateSubmission(item)
                  } yield ActivationResult.Activated(name, item)
            } yield status
            status.fold(err => ActivationResult.Error(name, err), a => a) :: acc
          case _ => acc
      }
      .getOrElse(acc)
  }
  result match
    case Left(err) => println(err)
    case Right(results) =>
      writeToCsv(results) */

def commandLine(): Unit = {
  given canvas: Canvas = Canvas(
    courseId = "72193",
    quizId = "103054",
    questionId = "2075376"
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
            canvas
              .getSubmissionStatus(cs.link, gs.time)
              .map {
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
              .value
              .get
              .getOrElse(report)

  }
  /* report match
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
      println(s"Submission twice: ${report.submissionMultipleTimesList}") */
}
