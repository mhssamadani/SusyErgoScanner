package testHandlers

import services.Module

import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should
import org.scalatest.propspec.AnyPropSpec
import play.api.inject.guice.GuiceApplicationBuilder
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.{Application, Mode}
import dao._

import java.io.File
import javax.inject.{Inject, Singleton}

@Singleton
class DaoContext @Inject()(val extractedBlockDAO: ExtractedBlockDAO,
                                  val extractionResultDAO: ExtractionResultDAO,
                                  val forkedResultDAO: ForkedResultDAO,
                                  val outputDAO: OutputDAO)


class TestSuite extends AnyPropSpec with should.Matchers with GuiceOneAppPerSuite with BeforeAndAfterAll {
  implicit override lazy val app: Application = new GuiceApplicationBuilder().
    configure(
      "slick.dbs.default.profile" -> "slick.jdbc.H2Profile$",
      "slick.dbs.default.driver" -> "slick.driver.H2Driver$",
      "slick.dbs.default.db.driver" -> "org.h2.Driver",
      "slick.dbs.default.db.url" -> "jdbc:h2:./test/db/scanner",
      "slick.dbs.default.db.user" -> "test",
      "slick.dbs.default.db.password" -> "test",
      "play.evolutions.autoApply" -> true,
      "slick.dbs.default.db.numThreads" -> 20,
      "slick.dbs.default.db.maxConnections" -> 20)
    .in(Mode.Test)
    .disable[Module]
    .build

  protected def DaoContext(implicit app: Application): DaoContext = {
    Application.instanceCache[DaoContext].apply(app)
  }

  override protected def afterAll(): Unit = {
    // delete db after test done.
    new File("./test/db/scanner.mv.db").deleteOnExit()
    new File("./test/db/scanner.trace.db").deleteOnExit()
  }
}
