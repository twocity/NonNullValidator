package me.twocities.nonnull.example

import me.twocities.nonnull.NonNullValidate
import me.twocities.nonnull.library.LibraryModel


private const val LIBRARY_JSON= """
{
"value": "internal value",
"id": 1024,
"list": ["a", "b", "c"],
"naiime": "name",
"map": {"a": 1, "b": 2}
}
"""

const val ANDROID_JSON = """
{
"library": $LIBRARY_JSON,
"name": "name",
"hasWiki": "wiki",
"description": "description",
"topics": ["a", "b", "c"]
}
"""

@NonNullValidate
data class AndroidModel(val library: LibraryModel,
    val name: String,
    val description: String?,
    val hasWiki: Boolean,
    val topics: List<String>
)
