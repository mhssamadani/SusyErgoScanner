package dao

import helpers.ErrorHandler.notFoundHandle
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.Inject
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

class ForkedResultDAO @Inject() (daoUtils: DAOUtils, extractedBlockDAO: ExtractedBlockDAO, outputDAO: OutputDAO,
                                 protected val dbConfigProvider: DatabaseConfigProvider)
                                (implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

    /**
     * Migrate blocks from a detected fork to alternate tables.
     * @param height height of block to be migrated
     * */
    def migrateBlockByHeight(height: Int): Unit = {

        val action = for {
            headerId <- extractedBlockDAO.getHeaderIdByHeightQuery(height)
            _ <- extractedBlockDAO.migrateForkByHeaderId(notFoundHandle(headerId))
            _ <- outputDAO.migrateForkByHeaderId(notFoundHandle(headerId))
        } yield {

        }
        val response = daoUtils.execTransact(action)
        Await.result(response, Duration.Inf)
    }
}
