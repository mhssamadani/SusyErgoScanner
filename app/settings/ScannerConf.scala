package settings

case class ScannerConf(
                        apiKey: String,
                        networkType: String,
                        explorerUrl: String,
                        serverUrl: String,
                        bestBlockId: String,
                        bankScriptAddress: String
                      )

