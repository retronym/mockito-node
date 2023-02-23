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
    try {
      val mock = Mockito.mock(classOf[Test], withSettings().defaultAnswer(RETURNS_SMART_NULLS))
      mock.m()
      assert(false)
    } catch {
      case ex: MockitoException =>
        assert(ex.getMessage.contains("overrides final method scala.collection.AbstractSeq.concat"))
    }
  }

  // Directly mocking Node fails as ByteBuddy emits a method that clashes with the final method in
  // a base class.
  private def mockingNodeFails(): Unit = {
    try {
      Mockito.mock[Node](classOf[Node])
      assert(false)
    } catch {
      case ex: MockitoException =>
        assert(ex.getMessage.contains("overrides final method scala.collection.AbstractSeq.concat"))
    }
  }

  // Standalone reproduction:
  private def mockingNFails(): Unit = {
    try {
      Mockito.mock[N](classOf[N])
      assert(false)
    } catch {
      case ex: MockitoException =>
        assert(ex.getMessage.contains("overrides final method demo.mockitonode.AbstractT.foo"))
    }
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
