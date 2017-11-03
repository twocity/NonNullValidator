# NonNullValidate

Validate class's non-null property after Gson's deserialization.

## Usage

__Use `NonNullValidate` to annotate your class:__

```kotlin
@NonNullValidate
data class Repo(val name: String,
	val description: String)
```
__Name of your TypeAdapterFactory:__

```groovy
kapt {
  arguments {
    arg("nonNullValidator.factoryName", "example.GsonNonNullValidator")
  }
}
```

```kotlin
val gson = GsonBuilder().registerTypeAdapterFactory(GsonNonNullValidator()).create()
// this will throw JsonSyntaxException, since `description` is absent
val repo = gson.fromJson("""{"name":"NonNullValidate"}""", Repo::class.java)
```

## Motivation

TODO

## Download

TODO

## Licence

MIT

