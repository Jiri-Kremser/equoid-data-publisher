package io.radanalytics.equoid

import org.scalatest._

import scala.util.Properties

class ParameterHandlingTest extends FlatSpec with Matchers {


  private def setEnv(key: String, value: String) = {
    val field = System.getenv().getClass.getDeclaredField("m")
    field.setAccessible(true)
    val map = field.get(System.getenv()).asInstanceOf[java.util.Map[java.lang.String, java.lang.String]]
    map.put(key, value)
  }

  setEnv("FOO_BAR", "secret")

  behavior of("DataPublisher.getProp")

  "it" should "return the value of environment variable" in {
    DataPublisher.getProp("FOO_BAR", "default") should === ("secret")
  }

  "it" should "return the value of system property if the environment variable doesn't exist" in {
    Properties.setProp("fooBaar", "sys-prop-value")
    DataPublisher.getProp("FOO_BAAR", "default") should === ("sys-prop-value")
    Properties.clearProp("fooBaar")
  }

  "it" should "return the default value if env. var and sys. prop are both undef" in {
    DataPublisher.getProp("FOO_BAAR", "default") should === ("default")
  }

  "it" should "return the value of environment variable even if the sys. prop exists (precedence)" in {
    Properties.setProp("fooBar", "sys-prop-value")
    DataPublisher.getProp("FOO_BAR", "default") should === ("secret")
  }

}
