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
    questionId = "2075376",
    cookies =
      "nmstat=cfe8174b-a965-3f84-51e6-d1a7fec5e7df; _ga_QHQ86LM5JZ=GS1.1.1683606973.1.0.1683607323.0.0.0; _ga_PDQN1TTJB7=GS1.1.1692427937.1.1.1692428005.0.0.0; _gcl_au=1.1.1631128518.1693194443; fpestid=g9yE_eKrVlJxe2AG-qGYG1k90KmNujN4fV2n2O4kcjCQIqXCjaVSRbRdqUOwmt09vYbU4w; _fbp=fb.1.1693318838854.417857518; _ga_NSB0YFP7QY=GS1.1.1693435664.1.0.1693435669.0.0.0; _ga_HJYMJN3PH0=GS1.1.1693498538.6.0.1693498540.58.0.0; cebs=1; _cc_id=47b965b0e15928d903a1a7c765c86d12; _ga_EJ5CY9NB8S=GS1.1.1694806892.6.0.1694806900.52.0.0; _ga_RJSVG73M20=GS1.1.1695784075.1.1.1695784096.0.0.0; _ga_07TCNE98Y0=GS1.1.1695850076.6.0.1695850076.0.0.0; _ga_WVEW83W2Q4=GS1.1.1696638363.1.1.1696638458.60.0.0; _ga_T6P0NVFNJD=GS1.1.1697037726.1.0.1697037732.0.0.0; _ga_6Q7PSKX96G=GS1.2.1697037742.1.0.1697037742.0.0.0; _ga=GA1.1.1380335681.1682352774; log_session_id=204fe43da98f3d85aee67d33f9a4d29c; cebsp_=27; _ga_BR8TQSBJZ3=GS1.1.1698156567.37.0.1698156570.0.0.0; _ce.s=v~c0455fa2e8cb2a021b4e405b100ca87d749d8e2c~vpv~1~v11.rlc~1693315894729~ir~1~lcw~1698156568655~v11.fhb~1698156568649~v11.lhb~1698156568649~gtrk.la~llz7o9v1~v11.cs~205175~v11.s~f1faffd0-7276-11ee-aa6f-f1bc9eb33e8c~v11.sla~1698156570262~lcw~1698156570262; _hp2_id.3001039959=%7B%22userId%22%3A%223553615765878325%22%2C%22pageviewId%22%3A%224458384553211521%22%2C%22sessionId%22%3A%226378249562731792%22%2C%22identity%22%3Anull%2C%22trackerVersion%22%3A%224.0%22%7D; _csrf_token=mziQAHzt%2B5lninpe9kBwMQTXDtVrWlU9v%2B%2BGb8OBubjsYOdkGZWy%2Bzf9SGqnb0dcUr9AgV4LM0j6pcBW98%2Fx%2FA%3D%3D; _legacy_normandy_session=gxVBl5lpZp6Acp7G8-CLtg+cCrIeaa5RGBCBoJtVJ_jedVYTCzAdjFDGbXYyA9p2l1kOJKeSo7I4Bsv2SbMb4DpV1lEvCrAobAEIq9poONynXhwLR9YSNvNniPh5sy9gsR3qbVNsuxJfPCiS8L_UhY-C1kGKmLcr1uoxrqlfux8Fw-9FNtbCHqLkmioWNIWB519mDAYjB69AtTcg3ZsciimNMAv-UH4pN6yjvv9TgLffOTdPSxEbrSoV_sFVYAUh64bp1848pn1sKpSv52MWTeUygf_gV6uH7tv_bqNuuTwLJTSXnJE_E9VnDXvzFxD6jd9o6n9IG0Xegm3zF4Wo3kXxadNRQiVz0DwMEs57SyltATgWCowcztCRBNDJ6T1RMvXFxGYrgfOXs4g5-uzSsmCYsVJLwF4QKJnIiJftUniYk6aG-vf1_pFvXqGG_nXVgwD1TpuICjd1CeH9tncd_vyFuOs4LBUzV-Bq9Pa3WI8Ew1z2jfHco5b-1Ugt5AADLDIebuHGo-aP1hkCPBl1G2hAxv5j20sadpoEJ8s8WfaSZITD6c9tPiIPSl97OSPz5fFzwgfAMpjq7_fqWfQfmfQRp4m7wWdCKOVIpdujvkjMtP93ZJcSSmgDkjgRSnvr1hUl0VLsPqYNmYrg_TSSo_wqwD80mSa-ePeqdbs6KQwUtxTTktEY-h7kGAxfEXVF-Dtn7GHcyMp3k2M2fFHi3SNHsLwBuNRvuX162_OhBGQrQu79foHc-WKnCnCIohaFiWNywEKb-LJLNhhv87g8UDh.p2vCZk3Ej6J4h0f0GI1HSxT81OY.ZTm_4g; canvas_session=gxVBl5lpZp6Acp7G8-CLtg+cCrIeaa5RGBCBoJtVJ_jedVYTCzAdjFDGbXYyA9p2l1kOJKeSo7I4Bsv2SbMb4DpV1lEvCrAobAEIq9poONynXhwLR9YSNvNniPh5sy9gsR3qbVNsuxJfPCiS8L_UhY-C1kGKmLcr1uoxrqlfux8Fw-9FNtbCHqLkmioWNIWB519mDAYjB69AtTcg3ZsciimNMAv-UH4pN6yjvv9TgLffOTdPSxEbrSoV_sFVYAUh64bp1848pn1sKpSv52MWTeUygf_gV6uH7tv_bqNuuTwLJTSXnJE_E9VnDXvzFxD6jd9o6n9IG0Xegm3zF4Wo3kXxadNRQiVz0DwMEs57SyltATgWCowcztCRBNDJ6T1RMvXFxGYrgfOXs4g5-uzSsmCYsVJLwF4QKJnIiJftUniYk6aG-vf1_pFvXqGG_nXVgwD1TpuICjd1CeH9tncd_vyFuOs4LBUzV-Bq9Pa3WI8Ew1z2jfHco5b-1Ugt5AADLDIebuHGo-aP1hkCPBl1G2hAxv5j20sadpoEJ8s8WfaSZITD6c9tPiIPSl97OSPz5fFzwgfAMpjq7_fqWfQfmfQRp4m7wWdCKOVIpdujvkjMtP93ZJcSSmgDkjgRSnvr1hUl0VLsPqYNmYrg_TSSo_wqwD80mSa-ePeqdbs6KQwUtxTTktEY-h7kGAxfEXVF-Dtn7GHcyMp3k2M2fFHi3SNHsLwBuNRvuX162_OhBGQrQu79foHc-WKnCnCIohaFiWNywEKb-LJLNhhv87g8UDh.p2vCZk3Ej6J4h0f0GI1HSxT81OY.ZTm_4g",
    token =
      "W8JHKnU4bm5Qy0BZbS8sp6NlhJB4d9rPUVjYSRXStJEsmjBOEEAnDAC8cm08ABvK9Q3KxE0mvLoUEp5wIZz81Q=="
  )

  given gradeScope: GradeScope =
    GradeScope(
      courseId = "576725",
      questionId = "28044790",
      assignmentId = "3480775",
      cookies =
        "_ga=GA1.2.1949017578.1694029043; __stripe_mid=2c43359b-c434-4392-8517-d916f0655d2ae88050; signed_token=bTlZOTVkYWVWZzBkMnlaNGlaa2xpVnVIZzdudHJYNitsUk1GMjJyTmdOcz0tLTErMlVZMkFpRlQwVTBhbHllTithU1E9PQ%3D%3D--cc0286c8d7934337043a98059095b5b78b06666e; remember_me=c1g5WFJ2NjZWRXhDZFVSbmpUR1pUUT09LS10T3QxVXg1Wk9aeEE2VUdZTUlacVVnPT0%3D--33e46c3a130448bc6399b8c04817d2a6bdedc8b3; apt.uid=AP-1BQVLBSZC216-2-1697431162899-44621736.0.2.42316860-d368-469b-a854-4c575f87a1c7; apt.sid=AP-1BQVLBSZC216-2-1698089600896-81673667; __stripe_sid=00120863-c18c-494e-9719-5e873e139c030e1ad7; _gradescope_session=WVlzT1JNMVZRWi96eWJBaWhzT2VDcm5RTmdCQ0hkV3VVOVFGZ1JZeU9OVXh0R1JUMkVVNS94dVowQVZ4TEtDWEpaY0JldnovaHZTMFZBR2xFMHhSa0hKcFJtUVlwSW81SkNWZFliWFdaS1VCY0lOK0s0SFFXYm9TV3hFMGRHOGN1cnFHN00wb0pIRHNnUDV0Q0RRSHNwcFpaVWp0cTR5YXIvbS9NRUNDNFRnM3R2QVdERFQ3aGVFUEJYTC9tR2VYdWczZkhrVW1Mbkl2RUhyNUtybjk1b0JjbFpiTkx0cGJxQXZ3V1dMeCthVXE2Vkd4blB2ZFpsY0ZWc3FiRUE4K1B3YnlVd0YrRUdXaktTeHhWWGtQZm5MeDUyRktITm82TXlIZDV3di9MVElxTW1janhXOUIyYzFsdlJhejhVZUZKUlRGTGN2T0pYTEd4WHZtU29VZXJJZzBYVnhZK3l0N01uRWxpK3d2UnhVeCtlS2U1T29kVk4za0xXeC81c2ZjNU52bTF2NWFaa3lXTVZ3KzliYjVzZDN3NzhhT1BuSmpMc0IvWEtUTklvTTJQd2g5NnNFR1kvTC92d2tvUlNJUDdUNHdqOWl4TmVaSTVBOHQ0dWRnd3Q1VEI2ODNaYys3RmoveG9NOWRHWi9ra0lBaS8yOFlkR0R4cWczc3QwZCs5dHk1VE1UZjRrN3M5WkhzWFhXalpwZXpEYkE3VVBxZnloT3dWVGgxQVRGdUZDR1hYQnMzNFNFWFZrQnpiMTBXSmxTd1dSZE1zWkxCbjd0Rm9wY2cxUT09LS1JRldRcXBiT0UrZGFzcGlpYUhxZGNBPT0%3D--14cb4faa35e14deff811d98678155c60745d1924",
      token =
        "sGN9mPEIS07414gDw4lcoSsAwRVnP953af4aNQUUYLJZ8KfWf8Muzw9PeTwkQjuivdv2yBWTw08d94cYcwVBcw=="
    )

  val report = for {
    manualList <- gradeScope.getManualGradingList
    scores <- gradeScope.getScores
    submissionList <- canvas.getStudentSubmission()
  } yield manualList.par.foldLeft(Report()) { (report, pair) =>
    given canvasSubmissions: Map[String, CanvasSubmission] = submissionList
    val (name, manualScore) = pair
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
