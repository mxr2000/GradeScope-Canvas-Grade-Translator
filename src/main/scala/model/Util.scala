package model

import java.time.LocalDateTime
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.OffsetDateTime
import io.circe.Decoder
import io.circe.generic.semiauto._
import io.circe.parser.decode
import io.circe.parser.parse

object Util {
  case class SubmissionItem(
      id: Long,
      created_at: LocalDateTime,
      owners: List[SubmissionOwner],
      show_path: String,
      active: Boolean,
      activate_path: String,
      can_activate: Boolean,
      score: String
  )

  case class SubmissionOwner(
      id: Long,
      active: Boolean,
      initials: String,
      name: String
  )

  def parseCreatedAt(created_at: String): LocalDateTime = {
    val formatter =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX")
    val offsetDateTime = OffsetDateTime.parse(created_at, formatter)
    offsetDateTime.toLocalDateTime
  }

  given ownerDecoder: Decoder[SubmissionOwner] =
    deriveDecoder[SubmissionOwner]

  given dateDecoder: Decoder[LocalDateTime] =
    Decoder.decodeString.map(parseCreatedAt)

  given decoder: Decoder[SubmissionItem] =
    deriveDecoder[SubmissionItem]

  def parseSubmissionItems(
      input: String
  ): Either[String, List[SubmissionItem]] =
    parse(input) match {
      case Right(json) =>
        json.hcursor
          .get[List[SubmissionItem]]("past_submissions")
          .left
          .map(err => s"${err.toString()}")
      case Left(err) => Left(s"${err.message}")
    }

  def findLatestBefore(
      items: List[SubmissionItem],
      dateTime: LocalDateTime
  ): Option[SubmissionItem] = {
    items
      .filter(
        _.created_at.isBefore(dateTime)
      )
      .maxByOption(_.created_at)
  }

}
