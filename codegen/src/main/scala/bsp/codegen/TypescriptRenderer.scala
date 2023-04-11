package bsp.codegen

import bsp.codegen.Def._
import bsp.codegen.EnumType.{IntEnum, StringEnum}
import bsp.codegen.Hint.Documentation
import bsp.codegen.Primitive._
import bsp.codegen.Type._
import bsp.codegen.dsl._
import cats.syntax.all._
import software.amazon.smithy.model.shapes.ShapeId

class TypescriptRenderer(baseRelPath: Option[os.RelPath]) {

  // scalafmt: { maxColumn = 120}
  def renderFile(definition: Def): Option[CodegenFile] = {
    val fileName = definition.shapeId.getName() + ".ts"
    baseRelPath.flatMap { base =>
      render(definition).map { lines =>
        CodegenFile(definition.shapeId, base / fileName, lines.render)
      }
    }
  }

  def render(definition: Def): Option[Lines] = {
    definition match {
      case Structure(shapeId, fields, hints)            => Some(renderStructure(shapeId, fields))
      case ClosedEnum(shapeId, enumType, values, hints) => None // no exemple of this in the spc
      case OpenEnum(shapeId, enumType, values, hints)   => Some(renderOpenEnum(shapeId, enumType, values))
      case Service(shapeId, operations, hints)          => None
    }
  }

  def renderStructure(shapeId: ShapeId, fields: List[Field]): Lines = {
    lines(
      block(s"export interface ${shapeId.getName()}")(
        newline,
        lines(fields.map(f => renderTSField(f)).intercalate(newline))
      )
    )
  }

  def renderOpenEnum[A](shapeId: ShapeId, enumType: EnumType[A], values: List[EnumValue[A]]): Lines = {
    val tpe = shapeId.getName()
    lines(
      block(s"export namespace $tpe") {
        newline
        values.map(renderStaticValue(enumType)).intercalate(newline)
      },
      newline
    )
  }

  def renderStaticValue[A](enumType: EnumType[A]): EnumValue[A] => Lines = {
    enumType match {
      case IntEnum =>
        (ev: EnumValue[Int]) =>
          lines(
            renderDocumentation(ev.hints),
            s"export const ${camelCase(ev.name).capitalize} = ${ev.value};"
          )

      case StringEnum =>
        (ev: EnumValue[String]) =>
          lines(
            renderDocumentation(ev.hints),
            s"""export const ${camelCase(ev.name).capitalize} = "${ev.value}""""
          )
    }
  }

  def camelCase(string: String): String = {
    val first :: rest = string.split(Array(' ', '_')).toList.map(_.toLowerCase)
    val changedRest = rest.map(w => w.take(1).toUpperCase + w.drop(1))
    val reunited = first :: changedRest
    reunited.mkString
  }

  def renderEnumValueDef[A](enumType: EnumType[A]): EnumValue[A] => String = {
    enumType match {
      case IntEnum    => (ev: EnumValue[Int]) => s"${ev.name}(${ev.value})"
      case StringEnum => (ev: EnumValue[String]) => s"""${ev.name}("${ev.value}")"""
    }
  }

  def renderTSField(field: Field): Lines = {
    val `:` = if (field.required) ": " else "?: "
    val maybeNonNull = if (field.required) lines("@NonNull") else empty
    lines(
      renderDocumentation(field.hints),
      field.name + `:` + renderType(field.tpe) + ";"
    )
  }

  def renderType(tpe: Type): String = tpe match {
    case TRef(shapeId)        => shapeId.getName()
    case TPrimitive(prim)     => renderPrimitive(prim)
    case TMap(key, value)     => ??? // Are maps even used in the BSP ?
    case TCollection(member)  => s"${renderType(member)}[]"
    case TUntaggedUnion(tpes) => tpes.map(renderType).mkString("|")
  }

  def renderPrimitive(prim: Primitive) = prim match {
    case PFloat     => "Float"
    case PDouble    => "Double"
    case PUnit      => "void"
    case PString    => "String"
    case PInt       => "Integer"
    case PDocument  => "any"
    case PBool      => "Boolean"
    case PLong      => "Long"
    case PTimestamp => "Long"
  }

  def renderDocumentation(hints: List[Hint]): Lines = hints
    .collectFold { case Documentation(string) =>
      val lines = string.split(System.lineSeparator())
      if (lines.nonEmpty) {
        lines(0) = "/** " + lines(0)
        val lastIndex = lines.length - 1
        lines(lastIndex) = lines(lastIndex) + " */"
      }
      Lines(lines.toList)
    }

}