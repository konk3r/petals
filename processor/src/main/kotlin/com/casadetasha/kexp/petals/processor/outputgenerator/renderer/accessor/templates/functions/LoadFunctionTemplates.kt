package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.templates.functions

import com.casadetasha.kexp.petals.processor.model.columns.PetalReferenceColumn
import com.casadetasha.kexp.petals.processor.model.AccessorClassInfo
import com.casadetasha.kexp.generationdsl.dsl.CodeTemplate
import com.casadetasha.kexp.generationdsl.dsl.FunctionTemplate
import com.casadetasha.kexp.generationdsl.dsl.ParameterTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.LoadMethodNames.LAZY_LOAD_ALL_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.LoadMethodNames.LOAD_ALL_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.LoadMethodNames.LOAD_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.LoadMethodNames.MAP_LAZY_MEMBER_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.CreateMethodNames.TRANSACTION_MEMBER_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.EagerLoadDependenciesMethodNames.COMPANION_EAGER_LOAD_DEPENDENCIES_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.ExportMethodNames.EXPORT_PETAL_METHOD_SIMPLE_NAME
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import org.jetbrains.exposed.sql.SizedIterable

internal fun createLoadFunctionTemplate(accessorClassInfo: AccessorClassInfo): FunctionTemplate =
    FunctionTemplate(
        name = LOAD_METHOD_SIMPLE_NAME,
        returnType = accessorClassInfo.className.copy(nullable = true),
    ) {
        collectParameterTemplates { accessorClassInfo.getLoadMethodParameters() }

        generateMethodBody(accessorClassInfo.getLoadMethodBody())
    }

private fun AccessorClassInfo.getLoadMethodParameters(): List<ParameterTemplate> {
    return listOf(
        ParameterTemplate(name = "id", typeName = idKotlinClassName),
        ParameterTemplate(name = "eagerLoad", typeName = Boolean::class.asClassName()) {
            defaultValue { CodeTemplate("false") }
        }
    )
}

private fun AccessorClassInfo.getLoadMethodBody(): CodeTemplate {
    val entitySimpleName = entityClassName.simpleName
    return when (petalColumns.any { it is PetalReferenceColumn }) {
        true -> CodeTemplate {
            generateControlFlowCode("return %M", TRANSACTION_MEMBER_NAME, endFlowString = "}") {
                generateControlFlowCode("when (eagerLoad)") {
                    generateCode (
                        format = "true -> %L.findById(id)?.$COMPANION_EAGER_LOAD_DEPENDENCIES_METHOD_SIMPLE_NAME()",
                        entitySimpleName
                    )

                    generateCode("false -> %L.findById(id)?.$EXPORT_PETAL_METHOD_SIMPLE_NAME()", entitySimpleName)
                }
            }
        }
        false -> CodeTemplate {
            generateControlFlowCode (
                prefix = "return %M", TRANSACTION_MEMBER_NAME,
                suffix = "?.$EXPORT_PETAL_METHOD_SIMPLE_NAME()"
            ) {
                generateCode("%L.findById(id)", entitySimpleName)
            }
        }
    }
}


internal fun createLoadAllFunctionTemplate(accessorClassInfo: AccessorClassInfo): FunctionTemplate =
    FunctionTemplate(
        name = LOAD_ALL_METHOD_SIMPLE_NAME,
        returnType = List::class.asClassName()
            .parameterizedBy(accessorClassInfo.className)
    ) {
        generateMethodBody {
            generateControlFlowCode("return %M", TRANSACTION_MEMBER_NAME) {
                generateCode (
                    "%L.all().map { it.$EXPORT_PETAL_METHOD_SIMPLE_NAME() }",
                    accessorClassInfo.entityClassName.simpleName
                )
            }
        }
    }


internal fun createLazyLoadAllFunctionTemplate(accessorClassInfo: AccessorClassInfo) =
    FunctionTemplate(
        name = LAZY_LOAD_ALL_METHOD_SIMPLE_NAME,
        returnType = SizedIterable::class.asClassName()
            .parameterizedBy(accessorClassInfo.className)
    ) {
        generateMethodBody {
            generateControlFlowCode("return %L.all().%M",
                accessorClassInfo.entityClassName.simpleName,
                MAP_LAZY_MEMBER_NAME
            ) {
                generateCode("it.$EXPORT_PETAL_METHOD_SIMPLE_NAME()")
            }
        }
    }
