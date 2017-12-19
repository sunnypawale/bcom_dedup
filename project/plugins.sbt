logLevel := Level.Warn



resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.1")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.6")