# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep Room entities
-keep class com.carlmanning.carlsbrain.data.local.entity.** { *; }

# Keep data classes used with kotlinx.serialization
-keepattributes *Annotation*
-dontwarn kotlinx.serialization.**

# Keep domain models
-keep class com.carlmanning.carlsbrain.domain.model.** { *; }
