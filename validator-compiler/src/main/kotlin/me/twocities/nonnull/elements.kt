package me.twocities.nonnull

import com.google.auto.common.MoreElements
import org.jetbrains.kotlin.serialization.Flags
import org.jetbrains.kotlin.serialization.ProtoBuf.Visibility.INTERNAL
import org.jetbrains.kotlin.serialization.ProtoBuf.Visibility.PUBLIC
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements


fun Element.asTypeElement(): TypeElement = MoreElements.asType(this)

fun TypeElement.packageName(elementUtils: Elements): String {
  return elementUtils.getPackageOf(this).qualifiedName.toString()
}

fun TypeElement.className(elementUtils: Elements): String {
  val packageLen = packageName(elementUtils).length + 1
  return this.qualifiedName.toString().substring(packageLen).replace('.', '$')
}


fun TypeElement.classData(elementUtils: Elements): ClassData {
  val annotation = getAnnotation(NonNullValidatorProcessor.KOTLIN_METADATA)
  val extractor = MetadataExtractor(annotation)
  val (nameResolver, classProto) = JvmProtoBufUtil.readClassDataFrom(extractor.d1(), extractor.d2())
  val propertyList = classProto.propertyList
      .filter {
        !it.returnType.nullable && propertyAccessible(it.flags)
      }
      .map {
        val name = nameResolver.getString(it.name)
        val nullable = it.returnType.nullable
        Property(name, nullable)
      }
  return ClassData(this, packageName(elementUtils), className(elementUtils), propertyList)
}

private fun propertyAccessible(flags: Int): Boolean {
  val visibility = Flags.VISIBILITY.get(flags)
  return visibility == PUBLIC || visibility == INTERNAL
}

private class MetadataExtractor(private val instance: Annotation) {
  private val klass = instance.annotationClass.java

  fun k(): Int {
    return klass.getDeclaredMethod("k").invoke(instance) as Int
  }

  fun mv(): IntArray {
    return klass.getDeclaredMethod("mv").invoke(instance) as IntArray
  }

  fun bv(): IntArray {
    return klass.getDeclaredMethod("bv").invoke(instance) as IntArray
  }

  fun xs(): String {
    return klass.getDeclaredMethod("xs").invoke(instance) as String
  }

  fun xi(): Int {
    return klass.getDeclaredMethod("xi").invoke(instance) as Int
  }

  @Suppress("UNCHECKED_CAST")
  fun d1(): Array<String> {
    return klass.getDeclaredMethod("d1").invoke(instance) as Array<String>
  }

  @Suppress("UNCHECKED_CAST")
  fun d2(): Array<String> {
    return klass.getDeclaredMethod("d2").invoke(instance) as Array<String>
  }
}