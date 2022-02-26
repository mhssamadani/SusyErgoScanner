package network

import org.ergoplatform.appkit.{NetworkType, RestApiErgoClient}
import play.api.Logger
import settings.Configuration.serviceConf
import javax.inject.Inject

class Client @Inject()(networkUtils: NetworkUtils) {
  private val logger: Logger = Logger(this.getClass)


  /**
   * Sets client for the entire app when the app starts, will use proxy if set in config
   *
   * @return current height of blockchain
   */
  def setClient(): Long = {
    try {
      logger.debug("hello")
      val networkType = if (serviceConf.networkType.toLowerCase.equals("mainnet")) NetworkType.MAINNET else NetworkType.TESTNET
      networkUtils.client = RestApiErgoClient.create(
        serviceConf.serverUrl, networkType, "", serviceConf.explorerUrl
      )
      networkUtils.getCtxClient(implicit ctx => {
        ctx.getHeight
      })

    } catch {
      case e: Throwable =>
        logger.error(s"Could not set client! ${e.getMessage}.")
        logger.debug(s"Could not set client! ${e.getMessage}.")
        0L
    }
  }

}
