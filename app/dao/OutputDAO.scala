package dao

import models.ExtractedOutputModel
import org.ergoplatform.ErgoBox
import org.ergoplatform.wallet.boxes.ErgoBoxSerializer
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait OutputComponent {
  self: HasDatabaseConfigProvider[JdbcProfile] =>

  import profile.api._

  class OutputTable(tag: Tag) extends Table[ExtractedOutputModel](tag, "OUTPUTS") {
    def boxId = column[String]("BOX_ID")

    def txId = column[String]("TX_ID")

    def headerId = column[String]("HEADER_ID")

    def value = column[Long]("VALUE")

    def creationHeight = column[Int]("CREATION_HEIGHT")

    def index = column[Short]("INDEX")

    def ergoTree = column[String]("ERGO_TREE")

    def timestamp = column[Long]("TIMESTAMP")

    def spent = column[Boolean]("SPENT", O.Default(false))

    def bytes = column[Array[Byte]]("BYTES")

    def additionalRegisters = column[String]("ADDITIONAL_REGISTERS")

    def additionalTokens = column[String]("ADDITIONAL_TOKENS")

    def txIndex = column[Int]("TX_INDEX")

    def sequence = column[Long]("SEQUENCE", O.AutoInc)

    def * = (boxId, txId, headerId, value, creationHeight, index, ergoTree, timestamp, bytes, additionalRegisters, additionalTokens, txIndex, sequence, spent) <> (ExtractedOutputModel.tupled, ExtractedOutputModel.unapply)

    def pk = primaryKey("PK_OUTPUTS", (boxId, headerId))
  }

  class OutputForkTable(tag: Tag) extends Table[ExtractedOutputModel](tag, "OUTPUTS_FORK") {
    def boxId = column[String]("BOX_ID")

    def txId = column[String]("TX_ID")

    def headerId = column[String]("HEADER_ID")

    def value = column[Long]("VALUE")

    def creationHeight = column[Int]("CREATION_HEIGHT")

    def index = column[Short]("INDEX")

    def ergoTree = column[String]("ERGO_TREE")

    def timestamp = column[Long]("TIMESTAMP")

    def spent = column[Boolean]("SPENT", O.Default(false))

    def bytes = column[Array[Byte]]("BYTES")

    def additionalRegisters = column[String]("ADDITIONAL_REGISTERS")

    def additionalTokens = column[String]("ADDITIONAL_TOKENS")

    def txIndex = column[Int]("TX_INDEX")

    def sequence = column[Long]("SEQUENCE", O.AutoInc)

    def * = (boxId, txId, headerId, value, creationHeight, index, ergoTree, timestamp, bytes, additionalRegisters, additionalTokens, txIndex, sequence, spent) <> (ExtractedOutputModel.tupled, ExtractedOutputModel.unapply)

    def pk = primaryKey("PK_OUTPUTS", (boxId, headerId))
  }
}

@Singleton()
class OutputDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider, daoUtils: DAOUtils)
                         (implicit executionContext: ExecutionContext)
  extends OutputComponent with ExtractedBlockComponent
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  val outputs = TableQuery[OutputTable]
  val outputsFork = TableQuery[OutputForkTable]
  val extractedBlocks = TableQuery[ExtractedBlockTable]

  /**
   * inserts a output into db
   *
   * @param output output
   */
  def insert(output: ExtractedOutputModel): Future[Unit] = db.run(outputs += output).map(_ => ())

  /**
   * create query for insert data
   *
   * @param outputs Seq of output
   */
  def insert(outputs: Seq[ExtractedOutputModel]): DBIO[Option[Int]] = this.outputs ++= outputs

  /**
   * @param headerId header id
   */
  def migrateForkByHeaderId(headerId: String): DBIO[Int] = {
    getByHeaderId(headerId)
      .map(outputsFork ++= _)
      .andThen(deleteByHeaderId(headerId))
  }

  /**
   * @param headerId header id
   * @return Output record(s) associated with the header
   */
  def getByHeaderId(headerId: String): DBIO[Seq[OutputTable#TableElementType]] = {
    outputs.filter(_.headerId === headerId).result
  }

  /**
   * @param headerId header id
   * @return Number of rows deleted
   */
  def deleteByHeaderId(headerId: String): DBIO[Int] = {
    outputs.filter(_.headerId === headerId).delete
  }


  def selectOutPutBoxesByHeight(offsetHeight: Int, limitHeight: Int): Seq[ErgoBox] = {
    val query = for {
      (outs, _) <-
        outputs join
          extractedBlocks.filter(block => {
            block.height < limitHeight && block.height >= offsetHeight
          }) on (_.headerId === _.id)
    } yield outs.bytes
    daoUtils.execAwait(query.result).map(ErgoBoxSerializer.parseBytes)
  }

  /**
   * @param boxId box id
   * @return Output timestamp for given box id.
   */
  def getBoxTimestampByBoxId(boxId: String): Long = {
    daoUtils.awaitResult(db.run(outputs.filter(_.boxId === boxId).result.headOption)).get.timestamp
  }

  /**
   * @param boxId box id
   * @return Output record(s) associated with the header
   */
  def getByBoxId(boxId: String): Future[Option[ExtractedOutputModel]] = {
    db.run(outputs.filter(_.boxId === boxId).result.headOption)
  }
}

