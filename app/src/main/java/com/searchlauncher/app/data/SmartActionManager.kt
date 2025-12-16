package com.searchlauncher.app.data

import android.content.Context

class SmartActionManager(private val context: Context) {

  fun checkSmartActions(query: String): List<SearchResult> {
    val results = mutableListOf<SearchResult>()

    // Phone Number Check
    val phoneMatcher = android.util.Patterns.PHONE.matcher(query)
    val isPhone = phoneMatcher.matches() && query.length >= 3

    // Check for explicit triggers
    val lowerQuery = query.lowercase()
    val isCallTrigger = lowerQuery.startsWith("call ")
    val isSmsTrigger = lowerQuery.startsWith("sms ") || lowerQuery.startsWith("text ")

    var phoneQuery = query
    if (isCallTrigger) {
      phoneQuery = query.substring(5).trim()
    } else if (isSmsTrigger) {
      if (lowerQuery.startsWith("sms ")) {
        phoneQuery = query.substring(4).trim()
      } else {
        phoneQuery = query.substring(5).trim()
      }
    }

    val isExplicitPhone =
      (isCallTrigger || isSmsTrigger) &&
        android.util.Patterns.PHONE.matcher(phoneQuery).matches() &&
        phoneQuery.length >= 3

    if (isPhone || isExplicitPhone) {
      val targetNumber = if (isExplicitPhone) phoneQuery else query

      // Call Action
      if (isPhone || isCallTrigger) {
        val callIcon = context.getDrawable(android.R.drawable.sym_action_call)
        results.add(
          SearchResult.Content(
            id = "smart_action_call_$targetNumber",
            namespace = "smart_actions",
            title = "Call $targetNumber",
            subtitle = "Phone",
            icon = callIcon,
            packageName = "com.android.dialer", // Best effort
            deepLink = "tel:$targetNumber",
            rankingScore = 100, // High priority
          )
        )
      }

      // Text Action
      if (isPhone || isSmsTrigger) {
        val messageIcon = context.getDrawable(android.R.drawable.sym_action_chat)
        results.add(
          SearchResult.Content(
            id = "smart_action_sms_$targetNumber",
            namespace = "smart_actions",
            title = "Text $targetNumber",
            subtitle = "SMS",
            icon = messageIcon,
            packageName = "com.android.mms", // Best effort
            deepLink = "sms:$targetNumber",
            rankingScore = 99, // Slightly lower than call
          )
        )
      }
    }

    // Email Check
    val emailMatcher = android.util.Patterns.EMAIL_ADDRESS.matcher(query)
    val isEmail = emailMatcher.matches()

    val isEmailTrigger = lowerQuery.startsWith("email ") || lowerQuery.startsWith("mailto ")
    var emailQuery = query
    if (isEmailTrigger) {
      if (lowerQuery.startsWith("email ")) {
        emailQuery = query.substring(6).trim()
      } else {
        emailQuery = query.substring(7).trim()
      }
    }

    val isExplicitEmail =
      isEmailTrigger && android.util.Patterns.EMAIL_ADDRESS.matcher(emailQuery).matches()

    if (isEmail || isExplicitEmail) {
      val targetEmail = if (isExplicitEmail) emailQuery else query
      val emailIcon = context.getDrawable(android.R.drawable.sym_action_email)
      results.add(
        SearchResult.Content(
          id = "smart_action_email_$targetEmail",
          namespace = "smart_actions",
          title = "Send Email to $targetEmail",
          subtitle = "Email",
          icon = emailIcon,
          packageName = "com.android.email", // Best effort
          deepLink = "mailto:$targetEmail",
          rankingScore = 100,
        )
      )
    }

    // URL Check
    val urlMatcher = android.util.Patterns.WEB_URL.matcher(query)
    if (urlMatcher.matches()) {
      val url =
        if (!query.startsWith("http://") && !query.startsWith("https://")) {
          "https://$query"
        } else {
          query
        }

      // Use a generic browser icon or similar if available, otherwise default
      // search icon
      val browserIcon =
        context.getDrawable(android.R.drawable.ic_menu_compass)
          ?: context.getDrawable(android.R.drawable.ic_menu_search)

      results.add(
        SearchResult.Content(
          id = "smart_action_url_$query",
          namespace = "smart_actions",
          title = "Open $query",
          subtitle = "Website",
          icon = browserIcon,
          packageName = "com.android.chrome", // Best effort, system handles
          // it
          deepLink = url,
          rankingScore = 98, // Slightly lower than direct actions
        )
      )
    }

    // Widget logic moved to Shortcuts.kt

    return results
  }
}
