package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.exposed

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.annotationparser.AnnotationParser.printThenThrowError
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey
import com.casadetasha.kexp.petals.processor.model.ParsedPetalSchema
import com.casadetasha.kexp.petals.processor.model.columns.LocalPetalColumn
import com.casadetasha.kexp.petals.processor.model.columns.PetalIdColumn
import com.casadetasha.kexp.petals.processor.model.columns.PetalReferenceColumn
import com.casadetasha.kexp.petals.processor.model.columns.PetalValueColumn
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.toMemberName
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.CodeTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.FileTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.ObjectTemplate.Companion.objectTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.PropertyTemplate.Companion.collectProperties
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.PropertyTemplate.Companion.createPropertyTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.SuperclassTemplate.Companion.superclassTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.exposed.ExposedClassesFileGenerator.Companion.EXPOSED_TABLE_PACKAGE
import com.squareup.kotlinpoet.*
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.dao.id.UUIDTable


internal fun FileTemplate.createExposedTableClassTemplate(
    packageName: String,
    baseName: String,
    tableName: String,
    schema: ParsedPetalSchema,
) =
    objectTemplate(className = ClassName(packageName, "${baseName}Table")) {
        superclassTemplate(className = schema.getTableSuperclass()) {
            collectConstructorParams {
                listOf(CodeTemplate("name = %S", tableName))
            }
        }

        collectProperties {
            schema.parsedLocalPetalColumns
                .filterNot { it is PetalIdColumn }
                .map { column ->
                    createPropertyTemplate( name = column.name, typeName = column.tablePropertyClassName) {
                        initializer { getColumnInitializationBlock(column) }
                    }
                }
        }
    }

private fun ParsedPetalSchema.getTableSuperclass(): ClassName {
    return when (this.primaryKeyType) {
        PetalPrimaryKey.INT -> IntIdTable::class.asClassName()
        PetalPrimaryKey.LONG -> LongIdTable::class.asClassName()
        PetalPrimaryKey.UUID -> UUIDTable::class.asClassName()
    }
}

private fun getColumnInitializationBlock(column: LocalPetalColumn): CodeTemplate {
    return when (column) {
        is PetalReferenceColumn -> getReferenceColumnInitializationBlock(column)
        is PetalValueColumn -> getValueColumnInitializationBlock(column)
        else -> printThenThrowError("INTERNAL LIBRARY ERROR: only value and reference petal columns allowed. Found ${column.kotlinType} for column ${column.name}.")
    }
}

private fun getValueColumnInitializationBlock(column: LocalPetalColumn): CodeTemplate {
    return when {
        (column.dataType.startsWith("CHARACTER VARYING")) -> getVarCharInitializationBlock(column)
        else -> getNonVarCharInitializationBlock(column)
    }
}

private fun getNonVarCharInitializationBlock(column: LocalPetalColumn): CodeTemplate {
    return when {
        column.isNullable -> {
            CodeTemplate(
                "%M(%S).%M()",
                getColumnCreationMemberName(column),
                column.name,
                MemberName(EXPOSED_TABLE_PACKAGE, "nullable")
            )
        }

        else -> {
            CodeTemplate(
                "%M(%S)",
                getColumnCreationMemberName(column),
                column.name
            )
        }
    }
}

private fun getVarCharInitializationBlock(column: LocalPetalColumn): CodeTemplate {
    val charLimit = column.dataType
        .removePrefix("CHARACTER VARYING(")
        .removeSuffix(")")

    return when {
        column.isNullable -> {
            CodeTemplate(
                "%M(%S, %L).%M()",
                MemberName(EXPOSED_TABLE_PACKAGE, "varchar"),
                column.name,
                charLimit,
                MemberName(EXPOSED_TABLE_PACKAGE, "nullable")
            )
        }

        else -> {
            CodeTemplate(
                "%M(%S, %L)",
                MemberName(EXPOSED_TABLE_PACKAGE, "varchar"),
                column.name,
                charLimit,
            )
        }
    }
}

private fun getReferenceColumnInitializationBlock(column: PetalReferenceColumn): CodeTemplate {
    val extension = if (column.isNullable) { ".nullable()" } else { "" }
    val baseCode = "%M(%S,·%M)$extension"
    return CodeTemplate(
            baseCode,
            getReferenceColumnCreationMemberName(),
            column.name,
            column.referencingTableClassName.toMemberName(),
        )
}

private fun getReferenceColumnCreationMemberName(): MemberName {
    return MemberName(EXPOSED_TABLE_PACKAGE, "reference")
}

private fun getColumnCreationMemberName(column: LocalPetalColumn): MemberName {
    val methodName = when (column.dataType) {
        "uuid" -> "uuid"
        "TEXT" -> "text"
        "INT" -> "integer"
        "BIGINT" -> "long"
        else -> AnnotationParser.printThenThrowError(
            "INTERNAL LIBRARY ERROR: unsupported column (${column.dataType})" +
                    " found while parsing Dao for table ${column.parentSchema.tableName}"
        )
    }

    return MemberName(EXPOSED_TABLE_PACKAGE, methodName)
}