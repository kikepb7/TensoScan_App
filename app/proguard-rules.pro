# Keep line numbers for readable crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# kotlinx-serialization, Retrofit, OkHttp, Room and Hilt/Dagger ship consumer rules; nothing else is needed yet.
# ML Kit and LiteRT rules are added together with those dependencies.
