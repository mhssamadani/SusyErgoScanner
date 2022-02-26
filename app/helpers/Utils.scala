package helpers

import org.ergoplatform.{ErgoAddress, ErgoAddressEncoder}
import org.ergoplatform.appkit.{Address, ErgoClient, ErgoContract, JavaHelpers, NetworkType, RestApiErgoClient}
import scorex.util.encode.Base16
import sigmastate.eval.SigmaDsl
import special.sigma.GroupElement

import java.math.BigInteger
import settings.Configuration
import sigmastate.basics.DLogProtocol.DLogProverInput


object node {
  val apiKey: String = Configuration.serviceConf.apiKey
  val url: String = Configuration.serviceConf.serverUrl
  val networkType: NetworkType = if (Configuration.serviceConf.networkType.toLowerCase.equals("mainnet")) NetworkType.MAINNET else NetworkType.TESTNET
  val explorer: String = Configuration.serviceConf.explorerUrl
}

object Utils {
  private val secureRandom = new java.security.SecureRandom

  def randBigInt: BigInt = new BigInteger(256, secureRandom)

  lazy val addressEncoder = new ErgoAddressEncoder(node.networkType.networkPrefix)

  def getContractAddress(contract: ErgoContract): String = {
    val ergoTree = contract.getErgoTree
    addressEncoder.fromProposition(ergoTree).get.toString
  }

  def getAddress(address: String): ErgoAddress = addressEncoder.fromString(address).get

  def getAddressFromSk(sk: BigInteger) = new Address(JavaHelpers.createP2PKAddress(DLogProverInput(sk).publicImage, addressEncoder.networkPrefix))

  val ergoClient: ErgoClient = RestApiErgoClient.create(node.url, node.networkType, node.apiKey, node.explorer)

  def randomId(): String = {
    val randomBytes = Array.fill(32)((scala.util.Random.nextInt(256) - 128).toByte)
    randomBytes.map("%02x" format _).mkString
  }

  def toByteArray(s: String): Array[Byte] = Base16.decode(s).get

  /**
   * convert hex represent to GroupElement
   * @param data String
   * @return a GroupElement object
   */
  def hexToGroupElement(data: String): GroupElement = {
    SigmaDsl.decodePoint(JavaHelpers.collFrom(toByteArray(data)))
  }

}
