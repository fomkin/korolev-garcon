/*
 * Copyright 2019 Aleksey Fomkin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package korolev.garcon

import scala.language.higherKinds
import scala.reflect.macros.blackbox

private[garcon] class DerivationMacros(val c: blackbox.Context) {

  import c.universe._

  def extension[F[_], S, M](implicit f: WeakTypeTag[F[_]],
                            s: WeakTypeTag[S],
                            m: WeakTypeTag[M]): Tree = {

    def materializeGetter(path: List[Path]) = {
      def aux(path: List[Path], that: Tree): Tree = path match {
        case Nil => q"Some($that)"
        case Path.CaseClass(name) :: xs => aux(xs, Select(that, name))
        case Path.SealedTrait(tpe, name) :: xs =>
          val tmpName = TermName(c.freshName(tpe.typeSymbol.name.toString))
          val select = Select(that, name)
          q"""$select match {
             case $tmpName: $tpe => ${aux(xs, Ident(tmpName))}
             case _ => None
           }"""
      }

      val stateName = TermName(c.freshName())
      val tree = aux(path.reverse, Ident(stateName))
      q"($stateName: $s) => $tree"
    }

    def materializeSetter(path: List[Path], dtpe: Type) = {
      val state = TermName(c.freshName())
      val demand = TermName(c.freshName())

      def aux(path: List[Path], acc: Tree): Tree = path match {
        case Path.CaseClass(name) :: Nil =>
          q"$acc.copy($name = $demand)"
        case Path.CaseClass(name) :: xs =>
          val content = aux(xs, q"$acc.$name")
          q"$acc.copy($name = $content)"
        case Path.SealedTrait(tpe, name) :: xs =>
          val matcher = {
            val temp = TermName(c.freshName(tpe.typeSymbol.name.toString))
            val next = aux(xs, Ident(temp))
            q"""
               $acc.$name match {
                 case $temp: $tpe => $next
                 case _ => $acc.$name
               }
            """
          }
          q"$acc.copy($name = $matcher)"
        case Nil => acc
      }

      val tree = aux(path.reverse, Ident(state))
      q"($state: $s, $demand: $dtpe) => $tree"
    }

    def scan(path: List[Path], tpe: Type): List[Tree] = {
      val params = tpe.decls.toList.collect { case m: MethodSymbol if m.isCaseAccessor => m }
      params.flatMap { param =>
        val tpe = param.typeSignature.finalResultType
        val symbol = tpe.typeSymbol
        val name = param.name.toString
        if (tpe <:< weakTypeOf[Demand[_, _, _]]) {
          val q :: r :: e :: Nil = tpe.typeArgs
          val finalPath = Path.CaseClass(TermName(name)) :: path
          //println(finalPath.reverse.mkString("."))
          val getter = materializeGetter(finalPath)
          val setter = materializeSetter(finalPath, tpe)
          val garcon = q"implicitly[korolev.garcon.Garcon[$f, $q, $r, $e]]"
          val entry =
            q"""
              korolev.garcon.Garcon.Entry[$f, $s, $q, $r, $e](
                garcon = $garcon,
                view = $getter,
                modify = $setter
              )
            """
          List(entry)
        } else if (isCaseClass(symbol)) {
          scan(Path.CaseClass(TermName(name)) :: path, tpe)
        } else if (isSealedTrait(symbol)) {
          symbol.asClass.knownDirectSubclasses flatMap { symbol =>
            if (isCaseClass(symbol)) {
              val tpe = symbol.asType.toType
              scan(Path.SealedTrait(tpe, TermName(name)) :: path, tpe)
            } else {
              // TODO add support for branchy hierarchy
              c.abort(c.enclosingPosition.pos, "Branchy hierarchy is not supported")
            }
          }
        } else {
          // This not case class, sealed trait or demand.
          // Skip.
          Nil
        }
      }
    }

    if (!isCaseClass(s.tpe.typeSymbol))
      c.abort(
        c.enclosingPosition.pos,
        s"Case class expected but ${s.tpe} given"
      )
    val entries = scan(Nil, s.tpe)
    q"korolev.garcon.Garcon.extension(..$entries)"
  }

  private sealed trait Path

  private object Path {

    case class CaseClass(fieldName: TermName) extends Path

    case class SealedTrait(tre: Type, fieldName: TermName) extends Path

  }

  private def isCaseClass(symbol: Symbol): Boolean =
    symbol.isClass && symbol.asClass.isCaseClass

  private def isSealedTrait(symbol: Symbol): Boolean =
    symbol.isClass && symbol.asClass.isTrait && symbol.asClass.isSealed
}
