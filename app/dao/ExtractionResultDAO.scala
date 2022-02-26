package dao

import models._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.Inject
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}


class ExtractionResultDAO @Inject() (daoUtils: DAOUtils, extractedBlockDAO: ExtractedBlockDAO,
                                     outputDAO: OutputDAO, protected val dbConfigProvider: DatabaseConfigProvider)
                                    (implicit executionContext: ExecutionContext)
    extends  HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  /**
  * Store extracted Block also store outputs, transactions are scanned according to rules into db as transactional.
   * @param createdOutputs : ExtractionOutputResultModel extracted outputs
   * @param extractedBlockModel: ExtractedBlockModel extracted block
   */
  def storeOutputsAndRelatedData(createdOutputs: Seq[ExtractedOutputModel],
                                 extractedBlockModel: ExtractedBlockModel): Unit = {
    val action = for {
        _ <- extractedBlockDAO.insert(Seq(extractedBlockModel))
        _ <- outputDAO.insert(createdOutputs)
    } yield {

    }
    val response = daoUtils.execTransact(action)
    Await.result(response, Duration.Inf)
  }
}
