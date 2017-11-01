package me.twocities.nonnull

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.INTERNAL
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import java.io.File


fun generateValidator(data: ClassData, dir: File) {
  val dataClassName = ClassName(data.packageName, data.className)
  val clazz = TypeSpec.classBuilder(ClassName(data.packageName, data.validatorName()))
      .addModifiers(INTERNAL)
      .addSuperinterface(ParameterizedTypeName.get(NonNullValidator::class.asTypeName(),
          dataClassName))

  val func = FunSpec.builder("validate")
      .addModifiers(OVERRIDE)
      .addParameter("t", dataClassName).apply {
    data.properties.filter { !it.nullable }.forEach {
      addStatement("requireNotNull(t.%L, { %S })",
          it.name, "`${data.className}${'$'}{'$'}${it.name}` declared as non-null, but was null.")
    }
  }.build()

  FileSpec.builder(data.packageName, data.validatorName())
      .addType(clazz.addFunction(func).build())
      .build()
      .writeTo(dir)
}

fun generateTypeAdapter(dataList: List<ClassData>, dir: File, className: String,
    packageName: String) {
  val adapterClassName = ClassName(packageName, className)
  val clazz = TypeSpec.classBuilder(adapterClassName)
      .addSuperinterface(TypeAdapterFactory::class)

  val t = TypeVariableName("T")
  val createFunc = FunSpec.builder("create")
      .addModifiers(OVERRIDE)
      .addAnnotation(AnnotationSpec.builder(Suppress::class)
          .addMember("names", "%S", "UNCHECKED_CAST")
          .build())
      .addParameter("gson", Gson::class)
      .addParameter("typeToken", ParameterizedTypeName.get(TypeToken::class.asTypeName(), t))
      .addTypeVariable(t)
      .returns(ParameterizedTypeName.get(TypeAdapter::class.asClassName(), t).asNullable())
      .addCode(codeBlockOfValidator(dataList))

  FileSpec.builder(packageName, className)
      .addType(clazz
          .addType(buildDelegateClass())
          .addFunction(createFunc.build()).build())
      .build()
      .writeTo(dir)
}

private fun codeBlockOfValidator(dataList: List<ClassData>): CodeBlock {
  val builder = CodeBlock.builder()
  builder.addStatement("val kClass = (typeToken.rawType as Class<*>).kotlin")
  dataList.forEachIndexed { index, data ->
    val className = ClassName(data.packageName, data.className)
    val validatorClassName = ClassName(data.packageName, data.validatorName())
    if (index == 0) {
      builder.beginControlFlow("if (%L::class == kClass)", className)
      builder.addStatement(
          "return DelegateTypeAdapter(gson.getDelegateAdapter(this, typeToken), %T() as NonNullValidator<T>)",
          validatorClassName)
      builder.endControlFlow()
    } else {
      builder.beginControlFlow("else if (%L::class == kClass)", className)
      builder.addStatement(
          "return DelegateTypeAdapter(gson.getDelegateAdapter(this, typeToken), %T() as NonNullValidator<T>)",
          validatorClassName)
      builder.endControlFlow()
    }
  }
  builder.addStatement("return null")
  return builder.build()
}

private fun buildDelegateClass(): TypeSpec {
  val t = TypeVariableName("T")
  val adapterClassName = ParameterizedTypeName.get(TypeAdapter::class.asClassName(), t)
  val validatorClassName = ParameterizedTypeName.get(NonNullValidator::class.asTypeName(), t)

  val clazz = TypeSpec.classBuilder("DelegateTypeAdapter")
      .addModifiers(PRIVATE)
      .primaryConstructor(FunSpec.constructorBuilder()
          .addParameter("delegate", adapterClassName)
          .addParameter("validator", validatorClassName)
          .build()
      )
      .addProperty(
          PropertySpec.builder("delegate", adapterClassName).addModifiers(PRIVATE).initializer(
              "delegate").build())
      .addProperty(
          PropertySpec.builder("validator", validatorClassName).addModifiers(PRIVATE).initializer(
              "validator").build())
      .superclass(adapterClassName)
      .addTypeVariable(t)

  val readFunc = FunSpec.builder("read")
      .addModifiers(OVERRIDE)
      .addParameter(ParameterSpec.builder("reader", JsonReader::class).build())
      .returns(t.asNullable())
      .addStatement("val value = delegate.read(reader)")
      .beginControlFlow("if (value != null)")
      .addStatement("validator.validate(value)")
      .endControlFlow()
      .addStatement("return value")
      .build()

  val writeFunc = FunSpec.builder("write")
      .addModifiers(OVERRIDE)
      .addParameter(ParameterSpec.builder("writer", JsonWriter::class).build())
      .addParameter(ParameterSpec.builder("value", t.asNullable()).build())
      .addStatement("delegate.write(writer, value)")
      .build()

  return clazz.addFunction(readFunc).addFunction(writeFunc).build()
}

