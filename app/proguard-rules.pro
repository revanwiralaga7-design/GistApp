# GistApp ProGuard Rules - Fix ParameterizedType crash

# Keep generic signatures and reflection metadata
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Retrofit
-keep class retrofit2.** { *; }
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations

# OkHttp
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Gson / Data Models
-keep class com.gistapp.data.model.** { *; }
-keepclassmembers class com.gistapp.data.model.** { *; }
-keepnames class com.gistapp.data.remote.** { *; }
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.* {}
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# EncryptedSharedPreferences
-keep class androidx.security.crypto.** { *; }
-keepnames class androidx.security.crypto.MasterKeys$* { *; }

# Reflection fix for ParameterizedType crash
-keepattributes ParameterizedType
-keepattributes ReflectionTarget
-keepattributes MethodParameters
