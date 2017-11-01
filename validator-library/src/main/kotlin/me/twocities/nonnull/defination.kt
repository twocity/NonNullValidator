package me.twocities.nonnull

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.CLASS

/**
 * Generate an implementation of [NonNullValidator], used to validate given class's non-null
 * properties.
 *
 */
@Target(CLASS)
@Retention(BINARY)
annotation class NonNullValidate

/**
 * Validate given value [T]'s non-null properties were not null after json deserialization.
 * The generated validator will implements this interface
 */
interface NonNullValidator<T> {
  fun validate(t: T)
}
