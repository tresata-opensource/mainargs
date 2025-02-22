import mill._, scalalib._, scalajslib._, scalanativelib._, publish._
import mill.scalalib.api.Util.isScala3
import scalalib._
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.1.4`
import de.tobiasroeser.mill.vcs.version.VcsVersion
import $ivy.`com.github.lolgab::mill-mima::0.0.9`
import com.github.lolgab.mill.mima._

val scala212 = "2.12.13"
val scala213 = "2.13.4"
val scala30 = "3.0.2"
val scala31 = "3.1.1"

val scala2Versions = List(scala212, scala213)

val scalaJSVersions = for {
  scalaV <- scala30 :: scala2Versions
  scalaJSV <- Seq("1.5.1")
} yield (scalaV, scalaJSV)

val scalaNativeVersions = for {
  scalaV <- scala2Versions
  scalaNativeV <- Seq("0.4.3")
} yield (scalaV, scalaNativeV)

trait MainArgsPublishModule extends PublishModule with CrossScalaModule with Mima {
  def publishVersion = VcsVersion.vcsState().format()
  def mimaPreviousVersions = Seq(
    VcsVersion
      .vcsState()
      .lastTag
      .getOrElse(throw new Exception("Missing last tag"))
  )
  // Remove after Scala 3 artifacts are published
  def mimaPreviousArtifacts = T{ if(isScala3(scalaVersion())) Seq() else super.mimaPreviousArtifacts() }
  def artifactName = "mainargs"

  def pomSettings = PomSettings(
    description = "Main method argument parser for Scala",
    organization = "com.lihaoyi",
    url = "https://github.com/lihaoyi/mainargs",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("lihaoyi", "mainargs"),
    developers = Seq(
      Developer("lihaoyi", "Li Haoyi","https://github.com/lihaoyi")
    )
  )

  def scalacOptions = super.scalacOptions() ++ (if (!isScala3(crossScalaVersion)) Seq("-P:acyclic:force") else Seq.empty)

  def scalacPluginIvyDeps = super.scalacPluginIvyDeps() ++ (if (!isScala3(crossScalaVersion)) Agg(ivy"com.lihaoyi::acyclic:0.2.0") else Agg.empty)

  def compileIvyDeps = super.compileIvyDeps() ++ (if (!isScala3(crossScalaVersion)) Agg(
      ivy"com.lihaoyi::acyclic:0.2.0",
      ivy"org.scala-lang:scala-reflect:$crossScalaVersion"
    ) else Agg.empty)

  def ivyDeps = Agg(
    ivy"org.scala-lang.modules::scala-collection-compat::2.4.4"
  ) ++ Agg(ivy"com.lihaoyi::pprint:0.6.6")
}

def scalaMajor(scalaVersion: String) = if(isScala3(scalaVersion)) "3" else "2"

trait Common extends CrossScalaModule {
  def millSourcePath = build.millSourcePath / "mainargs"
  def sources = T.sources(
    millSourcePath / "src",
    millSourcePath / s"src-$platform",
    millSourcePath / s"src-${scalaMajor(scalaVersion())}",
  )
  def platform: String
}

trait CommonTestModule extends ScalaModule with TestModule.Utest {
  def ivyDeps = Agg(ivy"com.lihaoyi::utest::0.7.11")
  def sources = T.sources(
    millSourcePath / "src",
    millSourcePath / s"src-$platform",
    millSourcePath / s"src-${scalaMajor(scalaVersion())}",
  )
  def platform: String
}


object mainargs extends Module {
  object jvm extends Cross[JvmMainArgsModule](scala30 :: scala2Versions: _*)
  class JvmMainArgsModule(val crossScalaVersion: String)
    extends Common with ScalaModule with MainArgsPublishModule {
    def platform = "jvm"
    object test extends Tests with CommonTestModule{
      def platform = "jvm"
      def ivyDeps = super.ivyDeps() ++ Agg(ivy"com.lihaoyi::os-lib:0.7.8")
    }
  }

  object js extends Cross[JSMainArgsModule](scalaJSVersions: _*)
  class JSMainArgsModule(val crossScalaVersion: String, crossJSVersion: String)
    extends Common with MainArgsPublishModule with ScalaJSModule {
    def platform = "js"
    def scalaJSVersion = crossJSVersion
    object test extends Tests with CommonTestModule{
      def platform = "js"
    }
  }

  object native extends Cross[NativeMainArgsModule](scalaNativeVersions: _*)
  class NativeMainArgsModule(val crossScalaVersion: String, crossScalaNativeVersion: String)
    extends Common with MainArgsPublishModule with ScalaNativeModule {
    def scalaNativeVersion = crossScalaNativeVersion
    def platform = "native"
    object test extends Tests with CommonTestModule{
      def platform = "native"
    }
  }
}

trait ExampleModule extends ScalaModule{
  def scalaVersion = "2.13.4"
  def moduleDeps = Seq(mainargs.jvm("2.13.4"))
}
object example{
  object hello extends ExampleModule
  object hello2 extends ExampleModule
  object caseclass extends ExampleModule
  object classarg extends ExampleModule
  object optseq extends ExampleModule

  object custom extends ExampleModule{
    def ivyDeps = Agg(ivy"com.lihaoyi::os-lib:0.7.1")
  }
  object vararg extends ExampleModule
  object vararg2 extends ExampleModule
}
