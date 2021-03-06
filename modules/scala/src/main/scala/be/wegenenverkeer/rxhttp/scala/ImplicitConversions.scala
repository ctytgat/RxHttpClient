package be.wegenenverkeer.rxhttp.scala

import java.util.concurrent.CompletionStage
import java.util.function.BiConsumer

import be.wegenenverkeer.rxhttp.{ServerResponse, ServerResponseElement, ClientRequest, RxHttpClient => JRxHttpClient}
import rx.lang.scala.Observable

import scala.concurrent.{Promise, Future}
import scala.util.{Failure, Success}


class RxHttpClient(val inner: JRxHttpClient) {

  import rx.lang.scala.JavaConversions.toScalaObservable
  import java.util.function.{ Function ⇒ JFunction}

  private def toJavaFunction[A, B](f: A => B) : JFunction[A,B]= new JFunction[A, B] {
    override def apply(a: A): B = f(a)
  }

  private def fromJavaFuture[B](jfuture: CompletionStage[B]) : Future[B] = {
    val p = Promise[B]()

    val consumer = new BiConsumer[B, Throwable] {
      override def accept(v: B, t: Throwable): Unit =
        if (t == null) p.complete(Success(v))
        else p.complete(Failure(t))
    }

    jfuture.whenComplete( consumer )
    p.future
  }

  def close() : Unit = this.inner.close()

  def execute[T](req: ClientRequest, transform: ServerResponse => T) : Future[T] =
    fromJavaFuture(inner.execute[T](req, toJavaFunction(transform)))

  def executeObservably[T](req: ClientRequest, transform : Array[Byte] => T) : Observable[T] =
    toScalaObservable(inner.executeObservably(req, toJavaFunction(transform)))


  def executeObservably(req: ClientRequest) : Observable[ServerResponseElement] =
    toScalaObservable(inner.executeObservably(req))


  def executeToCompletion[T](req: ClientRequest, transform: Function[ServerResponse,T]): Observable[T] =
    toScalaObservable(inner.executeToCompletion(req, toJavaFunction(transform)))

}


/**
 * Implicit conversion of RxHttpClient which adds methods that return [[rx.lang.scala.Observable]]s.
 *
 * Created by Karel Maesen, Geovise BVBA on 22/12/14.
 */
object ImplicitConversions {


  trait JavaClientWrapper {
    val inner: JRxHttpClient
    def asScala : RxHttpClient = new RxHttpClient(inner)
  }


  implicit def wrap( c : JRxHttpClient) : JavaClientWrapper = new JavaClientWrapper{
    val inner = c
  }

  implicit def unwrap(client: RxHttpClient) : JRxHttpClient = client.inner

}