package com.github.pqian.app

import com.github.pqian.app.Content.Content
import com.github.pqian.app.Topic.Topic
import slick.dbio.DBIO
import slick.driver.H2Driver.api._
import Tables._

import scala.concurrent.ExecutionContext

object DocumentsDAO {

  def newTicket(): String = java.util.UUID.randomUUID.toString

  def allDocuments: DBIO[Seq[Document]] = documents.result

  // create a document and return it
  def createDocument(content: Content, title: String, author: String, topic: Option[Topic]): DBIO[Document] = {

    val insertQuery = documents
      .map(r => (r.content, r.title, r.author, r.topic, r.status, r.ticket, r.fileName, r.message))
      .returning(documents.map(_.id))
      .into {
        case ((content, title, author, topic, status, ticket, fileName, message), id) =>
          Document(id, content, title, author, topic, status, ticket, fileName, message)
      }

    insertQuery += (content, title, author, topic, Status.initial, newTicket(), None, None)
  }

  def findDocument(documentId: Int)(implicit ec: ExecutionContext): DBIO[Option[Document]] = {
    documents.byId(documentId).result.headOption
  }

  def findDocument(ticket: String)(implicit ec: ExecutionContext): DBIO[Option[Document]] = {
    documents.byTicket(ticket).result.headOption
  }

  def updateDocumentFileName(documentId: Int, fileName: Option[String]): DBIO[Int] = {
    documents.byId(documentId).map(r => (r.status, r.fileName)).update(Status.pending, fileName)
  }

  def updateDocumentDone(documentId: Int): DBIO[Int] = {
    documents.byId(documentId).map(r => (r.status)).update(Status.done)
  }

  def updateDocumentErrorMessage(documentId: Int, errorMessage: Option[String]): DBIO[Int] = {
    documents.byId(documentId).map(r => (r.status, r.message)).update(Status.error, errorMessage)
  }

}
