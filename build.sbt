name := "coursier-private-repo-bootstrap"

organization := "me.chadrs"

version := "0.1"

scalaVersion := "2.12.10"

libraryDependencies ++= Seq(
  "com.github.pathikrit" %% "better-files" % "3.9.1",
  // would be better to just depend on the coursier-install module,
  // but I would have duplicated a lot of error handling
  "io.get-coursier" %% "coursier-cli" % "2.0.0-RC6-21",
  "com.github.alexarchambault" %% "case-app" % "2.0.0",
  "org.apache.maven" % "maven-settings-builder" % "3.6.3" // <-- be you didn't see this coming
)

resolvers += Resolver.typesafeIvyRepo("releases")
