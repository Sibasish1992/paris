package com.airbnb.paris.processor.models

import com.airbnb.paris.annotations.Styleable
import com.airbnb.paris.processor.framework.logError
import com.airbnb.paris.processor.framework.logWarning
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

internal class StyleableInfoExtractor {

    private val mutableModels = mutableListOf<StyleableInfo>()

    val models get() = mutableModels.toList()

    fun process(
        roundEnv: RoundEnvironment,
        classesToStyleableChildInfo: Map<TypeElement, List<StyleableChildInfo>>,
        classesToBeforeStyleInfo: Map<TypeElement, List<BeforeStyleInfo>>,
        classesToAfterStyleInfo: Map<TypeElement, List<AfterStyleInfo>>,
        classesToAttrsInfo: Map<TypeElement, List<AttrInfo>>,
        classesToStylesInfo: Map<Element, List<StyleInfo>>
    ): List<StyleableInfo> {
        val styleableElements = roundEnv.getElementsAnnotatedWith(Styleable::class.java)

        val classesMissingStyleableAnnotation =
            (classesToStyleableChildInfo + classesToAttrsInfo + classesToStylesInfo)
                .filter { (`class`, _) -> `class` !in styleableElements }
                .keys
        if (classesMissingStyleableAnnotation.isNotEmpty()) {
            logError(classesMissingStyleableAnnotation.first()) {
                "Uses @Attr, @StyleableChild and/or @Style but is not annotated with @Styleable."
            }
        }

        return styleableElements
            .mapNotNull {
                fromElement(
                    it as TypeElement,
                    classesToStyleableChildInfo[it] ?: emptyList(),
                    classesToBeforeStyleInfo[it] ?: emptyList(),
                    classesToAfterStyleInfo[it] ?: emptyList(),
                    classesToAttrsInfo[it] ?: emptyList(),
                    classesToStylesInfo[it] ?: emptyList()
                )
            }
            .also {
                mutableModels.addAll(it)
            }
    }

    private fun fromElement(
        element: TypeElement,
        styleableChildren: List<StyleableChildInfo>,
        beforeStyles: List<BeforeStyleInfo>,
        afterStyles: List<AfterStyleInfo>,
        attrs: List<AttrInfo>,
        styles: List<StyleInfo>
    ): StyleableInfo? {

        val baseStyleableInfo = BaseStyleableInfoExtractor().fromElement(element)

        if (baseStyleableInfo.styleableResourceName.isEmpty() && (attrs.isNotEmpty() || styleableChildren.isNotEmpty())) {
            logError(element) {
                "@Styleable is missing its value parameter (@Attr or @StyleableChild won't work otherwise)"
            }
            return null
        }

        if (baseStyleableInfo.styleableResourceName.isNotEmpty() && styleableChildren.isEmpty() && attrs.isEmpty()) {
            logWarning {
                "No need to specify the @Styleable value parameter if no class members are annotated with @Attr"
            }
        }

        return StyleableInfo(
            styleableChildren,
            beforeStyles,
            afterStyles,
            attrs,
            styles,
            baseStyleableInfo
        )
    }
}

/**
 * If [styleableResourceName] isn't empty then at least one of [styleableChildren] or [attrs] won't be
 * empty either
 */
internal class StyleableInfo(
    val styleableChildren: List<StyleableChildInfo>,
    val beforeStyles: List<BeforeStyleInfo>,
    val afterStyles: List<AfterStyleInfo>,
    val attrs: List<AttrInfo>,
    val styles: List<StyleInfo>,
    baseStyleableInfo: BaseStyleableInfo
) : BaseStyleableInfo(baseStyleableInfo) {

    /**
     * Applies lower camel case formatting
     */
    fun attrResourceNameToCamelCase(name: String): String {
        val formattedName = name.removePrefix("${styleableResourceName}_")
            .removePrefix("android_")
        return formattedName
            .foldRightIndexed("") { index, c, acc ->
                if (c == '_') {
                    acc
                } else {
                    if (index == 0 || formattedName[index - 1] != '_') {
                        c + acc
                    } else {
                        c.toUpperCase() + acc
                    }
                }
            }.decapitalize()
    }
}

