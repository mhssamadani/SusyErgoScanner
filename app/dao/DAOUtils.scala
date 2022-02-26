package dao

import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import spire.ClassTag

import java.util.concurrent.Executors
import javax.inject.Inject
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}

class DAOUtils @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api._

  /**
  * exec a dbio query as transactionally
   * @param dbio DBIO[T]
   * @tparam T Any
   * @return
   */
  def execTransact[T](dbio: DBIO[T]): Future[T] =
    db.run(dbio.transactionally)

  def execAwait[T](dbio: DBIO[T]): T = {
    val query = db.run(dbio)
    Await.result(query, Duration.Inf)
  }

  def awaitResult[T: ClassTag](variable: Future[T]): T = Await.result(variable, Duration.Inf)

  def shutdown: Future[Unit] = {
    db.close()
    implicit val context: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())
    Future {
      Thread.sleep(10000)
      System.exit(0)
    }
  }


}
