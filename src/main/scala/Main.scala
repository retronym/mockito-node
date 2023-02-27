package demo.mockitonode

import net.bytebuddy.description.method.MethodDescription
import org.mockito.Answers.{RETURNS_DEFAULTS, RETURNS_SMART_NULLS}
import org.mockito.Mockito.withSettings
import org.mockito.exceptions.base.MockitoException
import org.mockito.internal.creation.bytebuddy.{InlineByteBuddyMockMaker, InlineBytecodeGenerator}
import org.mockito.internal.framework.DefaultMockitoFramework
import org.mockito.invocation.MockHandler
import org.mockito.listeners.MockCreationListener
import org.mockito.mock.MockCreationSettings
import org.mockito.plugins.{MockMaker, MockitoPlugins}
import org.mockito.{Answers, Mockito, MockitoFramework}

import scala.xml.Node

object Main {
  def main(args: Array[String]): Unit = {
    mockingClassReferencingNodeFails()

    mockingClassReferencingNodeOkayWhenNotStubbingNode()
    mockingNodeFails()
    mockingNFails()
  }

  // Mockito by default creates a proxy class for scala.xml.Node when we call a method
  // in a mocked method that returns a Node. That triggers the failure indirectly.
  private def mockingClassReferencingNodeFails(): Unit = {
    val mock = Mockito.mock(classOf[Test], withSettings().defaultAnswer(RETURNS_SMART_NULLS))
    mock.m()
  }

  // Directly mocking Node fails as ByteBuddy emits a method that clashes with the final method in
  // a base class.
  private def mockingNodeFails(): Unit = {
    Mockito.mock[Node](classOf[Node])
  }

  // Standalone reproduction:
  private def mockingNFails(): Unit = {
    Mockito.mock[N](classOf[N])
  }

  private def mockingClassReferencingNodeOkayWhenNotStubbingNode(): Unit = {
    val mock = Mockito.mock(classOf[Test], withSettings().defaultAnswer(RETURNS_DEFAULTS))
    mock.m()
  }
}

class Test {
  def m(): scala.xml.Node = <a/>
}

trait U[A] {
  def foo[B >: A](): A = null.asInstanceOf[A]
}

trait T[A] extends U[A] {
  override final def foo[B >: A](): A = null.asInstanceOf[A]
}

class AbstractT[A] extends T[A]

trait BaseNS extends U[N] {
  // Akin to scala-xml's
  //  private[xml] trait ScalaVersionSpecificNodeSeq extends SeqOps[Node, immutable.Seq, NodeSeq] with
  //      StrictOptimizedSeqOps[Node, immutable.Seq, NodeSeq] { self: NodeSeq =>
  //    def concat(suffix: IterableOnce[Node]): NodeSeq = fromSpecific(iterator ++ suffix.iterator)
  def foo(): N = null
}

class NS extends AbstractT[N] with BaseNS

class N extends NS

class NodeMockMaker extends MockMaker {
  val delegate = new org.mockito.internal.creation.bytebuddy.ByteBuddyMockMaker

  def createMock[T](settings: MockCreationSettings[T], handler: MockHandler[_]): T = {
    val typeToMock = settings.getTypeToMock
    if (classOf[scala.xml.Document].isAssignableFrom(typeToMock))
      new scala.xml.Document().asInstanceOf[T]
    else if (classOf[scala.xml.NodeSeq].isAssignableFrom(typeToMock))
      scala.xml.Group(Nil).asInstanceOf[T]
    else if (classOf[NS].isAssignableFrom(typeToMock))
      new N().asInstanceOf[T]
    else
      delegate.createMock(settings, handler)
  }

  def getHandler(mock: Object): MockHandler[_] =
    delegate.getHandler(mock)

  def resetMock(mock: Object, newHandler: MockHandler[_], settings: MockCreationSettings[_]): Unit =
    delegate.resetMock(mock, newHandler, settings)

  def isTypeMockable(tpe: Class[_]): MockMaker.TypeMockability =
    delegate.isTypeMockable(tpe)
}
