package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor

import com.casadetasha.kexp.annotationparser.AnnotationParser.kaptKotlinGeneratedDir
import com.casadetasha.kexp.generationdsl.dsl.FileTemplate.Companion.generateFile
import com.casadetasha.kexp.petals.processor.model.AccessorClassInfo

internal class AccessorClassFileGenerator(
    private val accessorClassInfo: AccessorClassInfo
) {

    fun generateFile() =
        generateFile(
            directory = kaptKotlinGeneratedDir,
            packageName = PACKAGE_NAME,
            fileName = accessorClassInfo.className.simpleName
        ) {
            generateImport(importPackage = "org.jetbrains.exposed.dao", importName = "load")

            generateAccessorClass(accessorClassInfo)
        }.writeToDisk()

    companion object {
        const val PACKAGE_NAME = "com.casadetasha.kexp.petals.accessor"
    }
}