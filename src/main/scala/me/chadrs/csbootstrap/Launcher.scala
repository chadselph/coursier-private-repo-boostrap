package me.chadrs.csbootstrap

import java.io.File
import java.net.URI
import java.util.Properties

import better.files.File._
import caseapp._
import caseapp.core.RemainingArgs
import coursier.cli.install.InstallOptions
import org.apache.maven.settings.RuntimeInfo
import org.apache.maven.settings.building._
import org.apache.maven.settings.io.{
  DefaultSettingsReader,
  DefaultSettingsWriter
}
import org.apache.maven.settings.validation.DefaultSettingsValidator

import scala.collection.JavaConverters._
import scala.util.Try

case class Options(@Recurse installOptions: InstallOptions,
                   tryM2: Boolean = true,
                   tryNpm: Boolean = false,
                   saveCredentials: Boolean = false)

case class DiscoveredCredentials(host: String, user: String, pass: String) {
  override def toString: String = copy(pass = "***").toString
  def propertiesFileFormat(prefix: String) = {
    s"""
       |$prefix.host=$host
       |$prefix.username=$user
       |$prefix.password=$pass
       |$prefix.auto=true
       |
       |""".stripMargin
  }

  def toCmdline: String = s"$host $user:$pass"
}

object Launcher extends CaseApp[Options] {

  override def run(options: Options, remainingArgs: RemainingArgs): Unit = {
    val discoveredCredentials = {
      val maven = if (options.tryM2) harvestM2redentials() else Nil
      val npm = if (options.tryNpm) harvestNpmCredentials().toSeq else Nil
      (maven ++ npm).toSet
    }
    val dcToAdd = if (options.saveCredentials) {
      writeCsCredentials(discoveredCredentials)
      Seq()
    } else {
      discoveredCredentials.toSeq
    }

    coursier.cli.install.Install
      .run(installOpsWithCred(options.installOptions, dcToAdd), remainingArgs)
  }

  def writeCsCredentials(credentials: Set[DiscoveredCredentials]) = {
    val configDir = coursier.paths.CoursierPaths.configDirectory()
    val mainCredentialsFile = new File(configDir, "credentials.properties")
    if (mainCredentialsFile.exists()) {
      System.err.println(
        s"${mainCredentialsFile.getPath} already exists, not updating."
      )
    } else {
      val content = credentials.zipWithIndex
        .map {
          case (dc, index) =>
            dc.propertiesFileFormat(
              dc.host.takeWhile(_ != '.') + index.toString
            )
        }
        .mkString("\n")
      mainCredentialsFile.toPath.getParent.createDirectories()
      mainCredentialsFile.toPath.writeText(content)
      System.err.println(
        s"Wrote ${credentials.size} credentials to ${mainCredentialsFile.getPath}"
      )
    }
  }

  def harvestNpmCredentials() = {
    implicit class StringWithUnquote(val s: String) {
      def unquote: String = s.stripSuffix("\"").stripPrefix("\"")
    }
    val p = new Properties()
    val npmRc = home / ".npmrc"
    if (npmRc.exists) npmRc.inputStream.map(p.load)
    (p.get("registry"), p.get("_auth")) match {
      case (registry: String, b64auth: String) =>
        Try {
          val Array(user, apiKey) =
            new String(java.util.Base64.getDecoder.decode(b64auth.unquote))
              .split(':')
          DiscoveredCredentials(URI.create(registry).getHost, user, apiKey)
        }.toOption
      case _ =>
        None
    }
  }

  def harvestM2redentials() = {
    val builder = new DefaultSettingsBuilder(
      new DefaultSettingsReader(),
      new DefaultSettingsWriter,
      new DefaultSettingsValidator
    )
    val settings = builder
      .build(
        new DefaultSettingsBuildingRequest()
          .setUserSettingsFile(RuntimeInfo.DEFAULT_USER_SETTINGS_FILE)
      )
      .getEffectiveSettings
    settings.getServers.asScala.flatMap { server =>
      settings.getProfiles.asScala
        .flatMap(_.getRepositories.asScala)
        .filter(_.getId == server.getId)
        .map { rep =>
          DiscoveredCredentials(
            URI.create(rep.getUrl).getHost,
            server.getUsername,
            server.getPassword
          )
        }
    }
  }

  def installOpsWithCred(
    installOptions: InstallOptions,
    discoveredCredentials: Seq[DiscoveredCredentials]
  ): InstallOptions = {
    val newCreds = installOptions.cacheOptions.credentials ++ discoveredCredentials
      .map(_.toCmdline)
    installOptions.copy(
      cacheOptions = installOptions.cacheOptions.copy(credentials = newCreds)
    )
  }

}
