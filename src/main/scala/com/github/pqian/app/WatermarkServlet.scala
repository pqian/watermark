package com.github.pqian.app

import java.io.IOException

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import org.scalatra._
import org.scalatra.json._
import org.scalatra.servlet.{FileUploadSupport, MultipartConfig, SizeConstraintExceededException}
import DocumentsDAO._
import com.github.pqian.app.Content.Content
import com.github.pqian.app.Status.Status
import com.github.pqian.app.Topic.Topic
import scala.concurrent.duration._
import slick.driver.H2Driver.api._

import scala.concurrent.Future

class WatermarkServlet(db: Database, store: DocumentStore, system: ActorSystem, pdfActor: ActorRef)
  extends ScalatraServlet with FutureSupport with JacksonJsonSupport with FileUploadSupport with ContentEncodingSupport {

	configureMultipartHandling(MultipartConfig(
    	maxFileSize = Some(5 * 1024 * 1024),
    	maxRequestSize = Some(10 * 1024 * 1024)
  	))

  protected implicit lazy val jsonFormats = Formats.defaultFormats

  //override protected implicit def executor = scala.concurrent.ExecutionContext.global

  protected implicit def executor = system.dispatcher

  implicit val defaultTimeout = new Timeout(2 seconds)

  before() {
    contentType = formats("json")
  }

  error {
    case e: SizeConstraintExceededException => halt(500, "document file too large")
    case e: IOException =>
      log(e.toString)
      halt(500, "error occurred during document file operation.")
  }

  get("/") {
    redirect("/documents")
  }

  get("/documents") {
    new AsyncResult {
      val is = {
        db.run(allDocuments)
      }
    }
  }

  // return an document meatadata incl. status
  get("/documents/:ticket") {
    new AsyncResult {
      val is = {
        val ticket = params.as[String]("ticket")
        db.run(findDocument(ticket).transactionally) map {
          case Some(document) => document
          case None => NotFound("document metadata not found")
        }
      }
    }
  }

  get("/documents/:ticket/watermarkedfile") {
    new AsyncResult {
      val is = {
        val ticket = params.as[String]("ticket")
        db.run(findDocument(ticket).transactionally) map {
          case Some(doc) if (doc.status == Status.done && doc.fileName.nonEmpty) =>
            store.getWartermarkedFile(doc.id, doc.fileName) match {
              case Some(file) =>
                contentType = formats("pdf")
                // response.setHeader("Content-Disposition", f"""inline; filename="${file.getName}"""")
                // use this if you want to trigger a download prompt in the browsers
                response.setHeader("Content-Disposition", f"""attachment; filename="${file.getName}"""")
                file
              case None => NotFound("watermarked document not found")
            }
          case None => NotFound("document metadata not found")
        }
      }
    }
  }

  // create a new document
  post("/documents") {
    new AsyncResult {
      val is = {
        // halt on an error
        val content = (parsedBody \ "content").extractOpt[Content] getOrElse halt(BadRequest("invalid content, select \"book\" or \"journal\""))
        val title = (parsedBody \ "title").extractOpt[String] getOrElse halt(BadRequest("title is required"))
        val author = (parsedBody \ "author").extractOpt[String] getOrElse halt(BadRequest("author is required"))
        val topic = if (content == Content.book) (parsedBody \ "topic").extractOpt[Topic] match {
          case t@Some(_) => t
          case _ => halt(BadRequest("invalid topic for book, select \"business\",\"science\", or \"media\""))
        } else None

        db.run(createDocument(content, title, author, topic))
      }
    }
  }

  // update document file by ticket
  post("/documents/:ticket/file") {
    val ticket = params.as[String]("ticket")
    val file = fileParams.get("file") getOrElse halt(BadRequest("no file specified in request"))
    val fileName = file.getName
    val in = file.getInputStream

    new AsyncResult {
      val is = {
        db.run(findDocument(ticket).transactionally) map {
          case None =>
            halt(NotAcceptable("file cannot be accepted because document metadata not found"))
          case Some(doc) =>
            file.getContentType match {
              case Some("application/pdf") => doc.id
              case Some("application/octet-stream") if fileName.toLowerCase.endsWith(".pdf") => doc.id
              case _ => halt(NotAcceptable("only pdf allowed"))
            }
        } flatMap { docId => store.add(docId, fileName, in) } map { docId =>
          pdfActor ? docId
          Accepted("document file is saved, it will be watermarked.")
        }
      }
    }
  }


}
