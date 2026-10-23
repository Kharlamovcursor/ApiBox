-keep class ru.hdlabs.apibox.data.local.** { *; }

-keepattributes Signature,InnerClasses,EnclosingMethod
-keep class ru.hdlabs.apibox.BuildConfig { *; }

-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn javax.annotation.concurrent.GuardedBy
