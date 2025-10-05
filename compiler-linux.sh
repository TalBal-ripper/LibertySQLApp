jpackage \
  --name LibSQLApp \
  --input LibertyAppSQL/build/libs \
  --main-jar LibertyAppSQL-1.0-SNAPSHOT-all.jar \
  --main-class com.example.libertyappsql.Launcher \
  --type app-image \
  --java-options "--enable-preview"
