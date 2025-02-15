rootProject.name = "lnd-manageJ"
include("model")
include("backend")
include("balances")
include("caching")
include("configuration")
include("forwarding-history")
include("invoices")
include("onlinepeers")
include("payments")
include("pickhardt-payments")
include("privatechannels")
include("selfpayments")
include("statistics")
include("transactions")
include("web")
include("ui")
include("ui-demo")
include("grpc-adapter")
include("grpc-client")
include("application")

dependencyResolutionManagement {
    includeBuild("platform/")
}
