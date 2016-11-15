package com.github.pqian.app

import com.github.pqian.app.Content.Content
import com.github.pqian.app.Status.Status
import com.github.pqian.app.Topic.Topic

abstract class DBEnum extends Enumeration {

  import slick.driver.HsqldbDriver.api._
  import slick.driver.HsqldbDriver.MappedJdbcType

  implicit val enumMapper = MappedJdbcType.base[Value, Int](_.id, this.apply)
}

object Formats {

  import org.json4s.DefaultFormats
  import org.json4s.ext.EnumNameSerializer

  val defaultFormats = DefaultFormats + new EnumNameSerializer(Content) + new EnumNameSerializer(Topic) + new EnumNameSerializer(Status)
}

object Content extends DBEnum {
  type Content = Value
  val book, journal = Value
}

object Topic extends DBEnum {
  type Topic = Value
  val business, science, media = Value
}

object Status extends DBEnum {
  type Status = Value
  val initial, pending, done, error = Value
}

case class InputDocument(
                 content: Content,
                 title: String,
                 author: String,
                 topic: Option[Topic])

case class Document(
                 id: Int,
                 content: Content,
                 title: String,
                 author: String,
                 topic: Option[Topic],
                 status: Status,
                 ticket: String,
                 fileName: Option[String],
                 message: Option[String])
