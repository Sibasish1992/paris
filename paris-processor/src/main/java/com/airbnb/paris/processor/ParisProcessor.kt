package com.airbnb.paris.processor

import com.airbnb.paris.annotations.Styleable
import com.google.auto.service.AutoService
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic


@AutoService(Processor::class)
class ParisProcessor : AbstractProcessor() {

    companion object {

        internal val supportedAnnotations: Set<Class<out Annotation>>
            get() {
                return setOf(Styleable::class.java)
            }
    }

    private val loggedExceptions: MutableList<Exception> = ArrayList()

    private lateinit var filer: Filer
    private lateinit var messager: Messager
    private lateinit var elementUtils: Elements
    private lateinit var typeUtils: Types

    @Synchronized
    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        filer = processingEnv.filer
        messager = processingEnv.messager
        elementUtils = processingEnv.elementUtils
        typeUtils = processingEnv.typeUtils
    }

    override fun getSupportedAnnotationTypes(): Set<String> {
        val types: MutableSet<String> = LinkedHashSet()
        supportedAnnotations.mapTo(types) { it.canonicalName }
        return types
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
//        val styleableClasses = ArrayList()
//        for (element in roundEnv.getElementsAnnotatedWith(Styleable::class.java)) {
//            val classInfo = StyleableClassInfo.fromElement(element)
//            styleableClasses.add(classInfo)
//        }
//
//        try {
//            StyleClassesJavaWriter.writeFrom(filer, styleableClasses)
//        } catch (e: IOException) {
//            logError(e)
//        }
//
//        if (roundEnv.processingOver()) {
//            writeLoggedErrorsIfAny()
//        }

        return true
    }


    private fun logError(e: Exception) {
        loggedExceptions.add(e)
    }

    private fun writeLoggedErrorsIfAny() {
        if (loggedExceptions.isEmpty()) {
            return
        }

        // In case of a build failure only the first error is displayed, so this concatenates all of them in one
        // to help with debugging
        val concatenatedErrorsBuilder = StringBuilder()
        val iterator = loggedExceptions.iterator()
        while (iterator.hasNext()) {
            val exception = iterator.next()
            concatenatedErrorsBuilder.append("\n\n").append(exception.toString())
            iterator.remove()
        }
        messager.printMessage(Diagnostic.Kind.ERROR, concatenatedErrorsBuilder.toString())
    }
}
