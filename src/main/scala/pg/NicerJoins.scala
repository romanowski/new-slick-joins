package pg

import slick.ast.ElementSymbol
import slick.ast.Node
import slick.ast.ProductNode
import slick.ast.Select
import slick.jdbc.H2Profile.api._
import slick.lifted.FlatShapeLevel
import slick.lifted.ForeignKeyQuery
import slick.lifted.Shape
import slick.lifted.ShapeLevel
import slick.util.ConstArray

case class Join[F, From <: Table[F], On, T, To <: Table[T]](from: From, fk: ForeignKeyQuery[To, T])

trait NicerJoins[V, T <: Table[V]] {
  self: T =>

  /*private def contraintName(on: Rep[_]) = on.toNode.toString() + "_fk"


  def nicerJoin[On, WT, With <: Table[WT]](on: Rep[On], table: TableQuery[With])(f: With => Rep[On]) = {
   // this.join
    val a = foreignKey(contraintName(on),on, table)(f)
    Join(this, a)
  }*/


}

object NicerJoins {

  // type Join[A]

  implicit class Nicerrr[V, T](fromQuery: Query[T, V, Seq])(implicit s: Shape[FlatShapeLevel, T, V, T]) {

    /*def joinOn2[TV, To <: Table[TV]](k: T => ForeignKeyQuery[To, TV]): Query[Joined[To, T], Joined[TV, V], Seq] = {
      fromQuery.flatMap{
        v =>
          fromQuery.joinLeft(k(v).)
      }
        map(
        .joinLeft()
      fromQuery.flatMap {
        from =>
          val a = k(from)
          a.map(t => Joined(t, from))
      }
    }*/

    def joinOn[TV, To <: Table[TV]](k: T => ForeignKeyQuery[To, TV]): Query[Joined[To, T], Joined[TV, V], Seq] = {
      fromQuery.flatMap {
        from =>
          val a = k(from)
          a.map(t => Joined(t, from))
      }
    }
  }

}

case class Joined[A, B](to: A, from: B)

object Joined {

  @inline
  implicit final def joinedShape[Level <: ShapeLevel, M1, M2, U1, U2, P1, P2](implicit u1: Shape[_ <: Level, M1, U1, P1], u2: Shape[_ <: Level, M2, U2, P2]): Shape[Level, Joined[M1, M2], Joined[U1, U2], Joined[P1, P2]] = {
    new Shape[Level, Joined[M1, M2], Joined[U1, U2], Joined[P1, P2]] {
      override def pack(value: Mixed): Packed = Joined(u1.pack(value.to), u2.pack(value.from))

      override def packedShape = joinedShape(u1.packedShape, u2.packedShape)

      override def buildParams(extract: Any => Joined[U1, U2]): Packed =
        Joined(u1.buildParams(a => extract(a).to), u2.buildParams(a => extract(a).from))

      override def encodeRef(value: Mixed, path: Node): Any = Joined(
        u1.encodeRef(value.to, Select(path, ElementSymbol(1))),
        u2.encodeRef(value.from, Select(path, ElementSymbol(2)))
      )

      override def toNode(value: Mixed): Node = ProductNode(ConstArray(u1.toNode(value.to), u2.toNode(value.from)))
    }
  }


  case class Getter[+Field, -Base](get: Base => Field)

  object Getter extends LowPioriryGetter {

    // @implicitAmbiguous("Tail type ${A} conflicts in typed nested in ${Nested}")
    implicit def dropHead[Target, Head, Tail](implicit nested: Getter[Target, Tail]) =
      new Getter[Target, Joined[Head, Tail]](v => nested.get(v.from))

    implicit def pickFrom[Target, Head] = new Getter[Target, Joined[Head, Target]](_.from)

  }

  trait LowPioriryGetter {
    implicit def pickHead[Target, Tail] = new Getter[Target, Joined[Target, Tail]](_.to)

    implicit def foundConflict[Target](implicit nested: Getter[Target, Joined[Target, Target]]): Getter[Target, Joined[Target, Target]] = ???
  }

  implicit class GetterOps[T](val v: T) {
    def ~[X](implicit getter: Getter[X, T]) = getter.get(v)
    def as[X](implicit getter: Getter[X, T]) = getter.get(v)
  }

}
