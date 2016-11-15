import javax.servlet.ServletContext

import akka.actor.{ActorSystem, Props}
import com.github.pqian.app._
import org.scalatra._
import slick.driver.H2Driver.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class ScalatraBootstrap extends LifeCycle {

  val jdbcUrl = "jdbc:h2:mem:chapter10;DB_CLOSE_DELAY=-1"
  val jdbcDriverClass = "org.h2.Driver"
  val db = Database.forURL(jdbcUrl, driver = jdbcDriverClass)

  val store = DocumentStore("store", db)

  val system = ActorSystem()

  val pdfActor = system.actorOf(Props(PdfActor(store, db)))

  val app = new WatermarkServlet(db, store, system, pdfActor)

  override def init(context: ServletContext): Unit = {

    val res = db.run(DbSetup.createDatabase)

    Await.result(res, Duration(5, "seconds"))

    context.mount(app , "/*")
  }

  override def destroy(context: ServletContext): Unit = {
    db.close()
  }
}
