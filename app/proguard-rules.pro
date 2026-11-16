# Keep kotlinx.serialization serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class ru.ecobirch.fa.app.**$$serializer { *; }
-keepclassmembers class ru.ecobirch.fa.app.** {
    *** Companion;
}
-keepclasseswithmembers class ru.ecobirch.fa.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep osmdroid reflection if needed
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**
