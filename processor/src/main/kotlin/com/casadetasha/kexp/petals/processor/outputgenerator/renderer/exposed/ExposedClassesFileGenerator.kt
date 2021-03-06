package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.exposed

import com.casadetasha.kexp.annotationparser.AnnotationParser.kaptKotlinGeneratedDir
import com.casadetasha.kexp.petals.processor.model.ParsedPetalSchema
import com.casadetasha.kexp.petals.processor.model.PetalClasses
import com.casadetasha.kexp.generationdsl.dsl.FileTemplate.Companion.generateFile

internal class ExposedClassesFileGenerator(
    private val petalClasses: PetalClasses,
    private val className: String,
    private val tableName: String,
    private val schema: ParsedPetalSchema
) {

    companion object {
        const val EXPOSED_TABLE_PACKAGE = "org.jetbrains.exposed.sql.Table.Dual"
        const val PACKAGE_NAME: String = "com.casadetasha.kexp.petals"
    }

    fun generateFile() {
        generateFile(
            directory = kaptKotlinGeneratedDir,
            packageName = PACKAGE_NAME,
            fileName = "${className}Petals",
        ) {
            createExposedTableClassTemplate(
                packageName = PACKAGE_NAME,
                baseName = className,
                tableName = tableName,
                schema = schema
            )

            createExposedEntityClassTemplate(
                packageName = PACKAGE_NAME,
                petalClasses = petalClasses,
                schema = schema
            )
        }.writeToDisk()
    }
}
