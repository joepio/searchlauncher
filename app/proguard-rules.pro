# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep AppSearch entities
-keep class * extends androidx.appsearch.annotation.Document { *; }
