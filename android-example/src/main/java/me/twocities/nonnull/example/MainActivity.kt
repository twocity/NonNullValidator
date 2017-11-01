package me.twocities.nonnull.example

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.gson.GsonBuilder

import me.twocities.nonnull.library.LibraryTypeAdapterFactory

class MainActivity : AppCompatActivity() {
  private val gson by lazy {
    GsonBuilder()
        .registerTypeAdapterFactory(LibraryTypeAdapterFactory())
        .registerTypeAdapterFactory(GsonNonNullValidator())
        .create()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    val model = gson.fromJson(ANDROID_JSON, AndroidModel::class.java)
    Log.d("MainActivity", "$model")
  }

}
