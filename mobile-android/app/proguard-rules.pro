# Ktor and kotlinx.serialization keep their generated serializers by annotation; R8 needs telling.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class ph.attendance.** {
    *** Companion;
}
-keepclasseswithmembers class ph.attendance.** {
    kotlinx.serialization.KSerializer serializer(...);
}
