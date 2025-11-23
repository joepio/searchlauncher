package com.searchlauncher.app.data

import androidx.appsearch.annotation.Document
import androidx.appsearch.app.AppSearchSchema

@Document
data class AppSearchDocument(
  @Document.Namespace val namespace: String = "apps",
  @Document.Id val id: String, // Package Name
  @Document.Score val score: Int,
  @Document.StringProperty(
    indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES
  )
  val name: String, // App Name or Shortcut Label
  @Document.StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
  val intentUri: String? = null, // Intent URI for shortcuts
  @Document.StringProperty(
    indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES
  )
  val description: String? = null, // App Category or Description
  @Document.BooleanProperty
  val isAction: Boolean = false, // True if this is a direct action (not a search template)
  @Document.LongProperty val iconResId: Long = 0,
)
