package monoid

import parallelism.Par
import parallelism.Par._
import monoid.Monoid.Stub
import monoid.Monoid.Part

trait Monoid[A] {
    def op(a1: A, a2: A): A
    def zero: A
}

object Monoid {

    val stringMonoid = new Monoid[String] {
        def op(a1: String, a2: String) = a1 + a2
        def zero = ""
    }

    def listMonoid[A] = new Monoid[List[A]] {
        def op(a1: List[A], a2: List[A]) = a1 ++ a2
        def zero = Nil
    }

    val intAddition = new Monoid[Int] {
        def op(a1: Int, a2: Int): Int = a1 + a2
        def zero: Int = 0
    }

    val intMultiplication = new Monoid[Int] {
        def op(a1: Int, a2: Int): Int = a1 * a2
        def zero: Int = 1
    }

    val booleanOr = new Monoid[Boolean] {
        def op(a1: Boolean, a2: Boolean): Boolean = a1 || a2
        def zero: Boolean = false
    }

    val booleanAnd = new Monoid[Boolean] {
        def op(a1: Boolean, a2: Boolean): Boolean = a1 && a2
        def zero: Boolean = true
    }

    def optionMonoid[A] = new Monoid[Option[A]] {
        def op(a1: Option[A], a2: Option[A]) = a1.orElse(a2)
        def zero: Option[A] = None
    }

    def endoMonoid[A] = new Monoid[A => A] {
        def op(a1: A => A, a2: A => A) = a1 compose a2
        def zero = a => a
    }

    def foldMap[A,B](as: List[A], m: Monoid[B])(f: A => B): B =
        as.foldLeft(m.zero)((b, a) => m.op(b, f(a)))

    def foldRight[A, B](as: List[A])(z: B)(f: (A, B) => B): B =
        foldMap(as, endoMonoid[B])(f.curried)(z)

    def foldMapV[A,B](v: IndexedSeq[A], m: Monoid[B])(f: A => B): B =
        v match {
            case s if (s.length == 0) => m.zero
            case s if (s.length == 1) => f(s(0))
            case _ => {
                val mid = v.length / 2
                val (v1,v2) = v.splitAt(mid)
                m.op(foldMapV(v1, m)(f), foldMapV(v2, m)(f))
            }
        }

    def par[A](m: Monoid[A]): Monoid[Par[A]] = new Monoid[Par[A]] {
        def op(a1: Par[A], a2: Par[A]): Par[A] = Par.map2(a1, a2)(m.op)
        def zero: Par[A] = Par.unit(m.zero)
    }

    // def parFoldMap[A,B](v: IndexedSeq[A], m: Monoid[B])(f: A => B): Par[B] =
    //     Par.parMap(v)(f).flatMap { bs =>
    //         foldMapV(bs, par(m))(b => Par.lazyUnit(b))
    //     }

    sealed trait WC
    case class Stub(chars: String) extends WC
    case class Part(lStub: String, words: Int, rStub: String) extends WC

    val wcMonoid = new Monoid[WC] {
        def op(a1: WC, a2: WC): WC = (a1, a2) match {
            case (Stub(val1), Stub(val2)) => Stub(val1 + val2)
            case (Stub(val1), Part(l, w, r)) => Part(val1 + l, w, r)
            case (Part(l, w, r), Stub(val2)) => Part(l, w, r + val2)
            case (Part(l1, w1, r1), Part(l2, w2, r2)) => Part(l1, w1 + (if ((r1 + l2).isEmpty) 0 else 1) + w2, r2)
        }
        def zero = Stub("")
    }

    def count(s: String): Int = {
        foldMapV(s.toIndexedSeq, wcMonoid)(c => c match {
            case ' ' => Part("", 0, "")
            case a => Stub(a.toString())
        }) match {
            case Stub(chars) => chars.length min 1
            case Part(lStub, words, rStub) => lStub.length min 1 + words + rStub.length min 1
        }
    }

}