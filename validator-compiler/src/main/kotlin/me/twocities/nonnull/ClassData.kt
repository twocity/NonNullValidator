package me.twocities.nonnull

import javax.lang.model.element.TypeElement


data class ClassData(val element: TypeElement, val packageName: String, val className: String,
    val properties: List<Property>)

data class Property(val name: String, val nullable: Boolean)


fun ClassData.validatorName(): String = "${className}Validator_"
