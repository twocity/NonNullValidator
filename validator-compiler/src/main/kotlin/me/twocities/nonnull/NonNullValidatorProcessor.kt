package me.twocities.nonnull

import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier.ABSTRACT
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.tools.Diagnostic.Kind.ERROR

class NonNullValidatorProcessor : AbstractProcessor() {
  private lateinit var messenger: Messager
  private lateinit var elementUtil: Elements
  private lateinit var kaptGeneratedDir: File
  private lateinit var factoryFqName: String

  companion object {
    private val KAPT_KOTLIN_GENERATED_OPTION = "kapt.kotlin.generated"
    private val FACTORY_NAME_OPTION = "nonNullValidator.factoryName"
    private val DEFAULT_FACTORY = "me.twocities.nonnull.validate.NonNullValidateTypeAdapterFactory"
    @Suppress("UNCHECKED_CAST")
    val KOTLIN_METADATA = Class.forName("kotlin.Metadata") as Class<out Annotation>
  }

  override fun init(processingEnv: ProcessingEnvironment) {
    messenger = processingEnv.messager
    elementUtil = processingEnv.elementUtils
    val generatedPath = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION] ?: run {
      logError("Can't find `kapt.kotlin.generated` in kapt options")
      return
    }
    kaptGeneratedDir = File(generatedPath).apply { mkdirs() }
    factoryFqName = processingEnv.options[FACTORY_NAME_OPTION] ?: DEFAULT_FACTORY
  }

  override fun process(annotations: Set<TypeElement>,
      roundEnv: RoundEnvironment): Boolean {
    val elements = roundEnv.getElementsAnnotatedWith(NonNullValidate::class.java)
    val validDataClasses = elements.filter {
      !validateElement(it)
    }.map { it.asTypeElement().classData(elementUtil) }

    if (validDataClasses.isNotEmpty()) {
      // generate validators
      validDataClasses.forEach { generateValidator(it, kaptGeneratedDir) }

      // generate TypeAdapterFactory
      val fqName = factoryFqName.trim()
      if (fqName.isBlank()) {
        error("$FACTORY_NAME_OPTION can't be empty.")
      } else {
        val index = factoryFqName.lastIndexOf('.')
        if (index == -1) {
          generateTypeAdapter(validDataClasses, kaptGeneratedDir, fqName, "")
        } else {
          generateTypeAdapter(validDataClasses, kaptGeneratedDir,
              factoryFqName.substring(index + 1, factoryFqName.length),
              factoryFqName.substring(0, index))
        }
      }
    }
    return false
  }


  private fun validateElement(element: Element): Boolean {
    var hasError = false
    if (element.kind != ElementKind.CLASS) {
      hasError = true
      logError("@${NonNullValidate::class.simpleName} can only annotated with class.", element)
    }
    if (element.modifiers.hasAnyOf(setOf(PRIVATE, ABSTRACT))) {
      hasError = true
      logError("Class can't be private or abstract", element)
    }
    val metadataAnnotation = element.getAnnotation(KOTLIN_METADATA)
    if (metadataAnnotation == null) {
      hasError = true
      logError("$element is not a kotlin class", element)
    }
    return hasError
  }

  override fun getSupportedAnnotationTypes(): Set<String> {
    return setOf(NonNullValidate::class.java.canonicalName)
  }

  override fun getSupportedSourceVersion(): SourceVersion {
    return SourceVersion.latestSupported()
  }

  private fun logError(msg: String, element: Element) {
    messenger.printMessage(ERROR, msg, element)
  }

  private fun logError(msg: String) {
    messenger.printMessage(ERROR, msg)
  }

  fun <T> Iterable<T>.hasAnyOf(that: Iterable<T>) = this.any { other -> that.any { other == it } }
}