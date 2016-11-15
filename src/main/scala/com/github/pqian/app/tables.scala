package com.github.pqian.app

import com.github.pqian.app.Content.Content
import com.github.pqian.app.Status.Status
import com.github.pqian.app.Topic.Topic
import slick.driver.H2Driver.api._

object Tables {

  class Documents(tag: Tag) extends Table[Document](tag, "DOCUMENTS") {
    def id            = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def content       = column[Content]("CONTENT")
    def title         = column[String]("TITLE")
    def author        = column[String]("AUTHOR")
    def topic         = column[Option[Topic]]("TOPIC")
    def status        = column[Status]("STATUS", O.Default(Status.initial))
    def ticket        = column[String]("TICKET")
    def fileName      = column[Option[String]]("FILE_NAME")
    def message       = column[Option[String]]("MESSAGE")

    def * = (id, content, title, author, topic, status, ticket, fileName, message) <> (Document.tupled, Document.unapply)

    def idx = index("idx_ticket", (ticket), unique = true)
  }

  // table queries

  val documents = TableQuery[Documents]

  // add some useful queries to documents queries

  implicit class DocumentsQueryExtensions[C[_]](query: Query[Documents, Document, C]) {

    val insertTuple = query
      .map(r => (r.content, r.title, r.author, r.topic, r.status, r.ticket, r.fileName, r.message))
      .returning(documents.map(_.id))
      .into {
        case ((content, title, author, topic, status, ticket, fileName, message), id) =>
          Document(id, content, title, author, topic, status, ticket, fileName, message)
      }

    val sortById = query.sortBy(_.id.asc)

    def byId(id: Int) = query.filter(_.id === id)

    def byTicket(ticket: String) = query.filter(_.ticket === ticket)

    def byContent(content: Content) = query.filter(_.content === Content.book)

    def byTitle(title: String) = query.filter(_.title === title)

    def byAuthor(author: String) = query.filter(_.author === author)

    def byTopic(topic: Topic) = query.filter(_.topic === topic)

  }

}
