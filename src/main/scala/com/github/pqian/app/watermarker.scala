package com.github.pqian.app

import java.io.{FileInputStream, FileOutputStream}

import akka.actor.{Actor, ActorLogging}
import akka.actor.Actor.Receive
import DocumentsDAO._
import com.github.pqian.app.Status.Status
import com.itextpdf.text.{Element, Font, Phrase}
import com.itextpdf.text.Font.FontFamily
import com.itextpdf.text.pdf.{ColumnText, PdfGState, PdfReader, PdfStamper}
import slick.driver.H2Driver.api._
import org.json4s.jackson.Serialization._

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by pqian on 14/11/16.
  */
case class PdfActor(store: DocumentStore, db: Database) extends Actor with ActorLogging {

  implicit val jsonFormats = Formats.defaultFormats

  override def receive: Receive = {
    case docId: Int =>
      db.run(findDocument(docId).transactionally) map {
        case Some(doc) => addWatermark(doc) match {
          case Right(_) =>
            db.run(updateDocumentDone(docId))
          case Left(errorMessage) =>
            db.run(updateDocumentErrorMessage(docId, Some(errorMessage)))
            log.error(errorMessage)
        }
        case None => log.warning(s"document not found by docId $docId")
      }
    case msg => log.warning(s"this was not docId: $msg")
  }

  def addWatermark(doc: Document): Either[String, Status] = {
    if (doc.status == Status.pending && doc.fileName.nonEmpty) {
      val file = store.asFile(doc.id, doc.fileName.get)
      if (file.exists()) {
        val reader = new PdfReader(new FileInputStream(file))
        val watermarkedFile = store.asWartermarkedFile(doc.id, doc.fileName.get)
        val stamper = new PdfStamper(reader, new FileOutputStream(watermarkedFile))

        // addition info
        val info = reader.getInfo()
        info.put("Title", doc.title)
        info.put("Author", doc.author)
        if (doc.content == Content.book)
          info.put("Topic", if (doc.topic.nonEmpty) doc.topic.get.toString else "Unknown")
        stamper.setMoreInfo(info)

        // text watermark
        val props = Map("content" -> doc.content, "title" -> doc.title, "author" -> doc.author)
        val props2 = if (doc.content == Content.book) props + ("topic" -> doc.topic) else props
        val text = write(props2)

        val f = new Font(FontFamily.HELVETICA, 10)
        val p = new Phrase(text, f)
        // transparency
        val gs1 = new PdfGState()
        gs1.setFillOpacity(0.5f)
        // loop over every page
        for (i <- 1 to reader.getNumberOfPages()) {
          val pagesize = reader.getPageSizeWithRotation(i)
          val x = (pagesize.getLeft() + pagesize.getRight()) / 2
          val y = (pagesize.getTop() + pagesize.getBottom()) * 19 / 20
          val over = stamper.getOverContent(i)
          over.saveState()
          over.setGState(gs1)
          ColumnText.showTextAligned(over, Element.ALIGN_CENTER, p, x, y, 0);
          over.restoreState();
        }

        stamper.close()
        reader.close()
        log.info(s"document file watermarked with text and more info: $watermarkedFile")
        Right(Status.done)
      } else {
        Left(s"original doc file not found: $file")
      }
    } else {
      Left(s"document file not found by docId ${doc.id}")
    }
  }

}
