package network

import org.ergoplatform.appkit.{Address, BlockchainContext, ErgoClient, InputBox}
import play.api.Logger

import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.collection.JavaConverters._


@Singleton
class NetworkUtils @Inject()() {
  var client: ErgoClient = _

  def getCtxClient[T](f: BlockchainContext => T): T = {
    client.execute { ctx =>
      f(ctx)
    }
  }

  /**
   * @return current height of the blockchain
   */
  def getHeight: Long = {
    getCtxClient(ctx => ctx.getHeight)
  }

  /**
   * @param boxId box id of the box we wish to get
   * @return box as InputBox
   */
  def getUnspentBoxById(boxId: String): InputBox = {
    getCtxClient { implicit ctx =>
      ctx.getBoxesById(boxId).headOption.getOrElse(throw new Exception("No box found"))
    }
  }

}
