# ProGuard rules for the BubbleBrowser app
# Suppress warnings for missing annotations
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.ParametersAreNonnullByDefault

# OpenNLP related rules
-dontwarn org.osgi.framework.**
-dontwarn org.osgi.util.tracker.**
-keep class opennlp.tools.** { *; }
-keepclassmembers class opennlp.tools.** { *; }