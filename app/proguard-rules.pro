# ProGuard rules for the QuickBrowser app
# Suppress warnings for missing annotations
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.ParametersAreNonnullByDefault

# OpenNLP related rules
-dontwarn org.osgi.framework.**
-dontwarn org.osgi.util.tracker.**
-keep class opennlp.tools.** { *; }
-keepclassmembers class opennlp.tools.** { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ResourceDecoder$* {
    **[] $VALUES;
    public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
    *** rewind();
}

# Allow loading resources from assets
-keep class com.bumptech.glide.load.data.AssetPathFetcher
-keep class com.bumptech.glide.load.data.LocalUriFetcher

# Allow loading resources from raw folder
-keep class com.bumptech.glide.load.data.StreamLocalUriFetcher
-keep class com.bumptech.glide.load.data.FileDescriptorLocalUriFetcher