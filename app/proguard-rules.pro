# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn java.awt.Point
-dontwarn java.awt.image.BufferedImage
-dontwarn java.awt.image.DataBuffer
-dontwarn java.awt.image.DataBufferByte
-dontwarn java.awt.image.Raster
-dontwarn java.awt.image.RenderedImage
-dontwarn java.awt.image.WritableRaster
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
-dontwarn java.lang.management.ThreadMXBean
-dontwarn javax.imageio.ImageIO
-dontwarn javax.naming.NamingEnumeration
-dontwarn javax.naming.NamingException
-dontwarn javax.naming.directory.Attribute
-dontwarn javax.naming.directory.Attributes
-dontwarn javax.naming.directory.DirContext
-dontwarn javax.naming.directory.InitialDirContext
-dontwarn javax.naming.directory.SearchControls
-dontwarn javax.naming.directory.SearchResult
-dontwarn net.jcip.annotations.GuardedBy
-dontwarn org.bouncycastle.asn1.cms.Attribute
-dontwarn org.bouncycastle.asn1.cms.ContentInfo
-dontwarn org.bouncycastle.asn1.cms.IssuerAndSerialNumber
-dontwarn org.bouncycastle.asn1.cms.SignedData
-dontwarn org.bouncycastle.asn1.cms.SignerIdentifier
-dontwarn org.bouncycastle.asn1.cms.SignerInfo
-dontwarn org.bouncycastle.asn1.eac.EACObjectIdentifiers
-dontwarn org.bouncycastle.asn1.icao.DataGroupHash
-dontwarn org.bouncycastle.asn1.icao.LDSSecurityObject
-dontwarn org.bouncycastle.asn1.icao.LDSVersionInfo
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.fusesource.leveldbjni.JniDBFactory
-dontwarn org.iq80.leveldb.DB
-dontwarn org.iq80.leveldb.DBException
-dontwarn org.iq80.leveldb.DBFactory
-dontwarn org.iq80.leveldb.DBIterator
-dontwarn org.iq80.leveldb.Options
-dontwarn org.iq80.leveldb.ReadOptions
-dontwarn org.iq80.leveldb.Snapshot
-dontwarn org.iq80.leveldb.WriteBatch
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn org.slf4j.impl.StaticMDCBinder
-dontwarn org.slf4j.impl.StaticMarkerBinder

-keep class com.onekosmos.blockid.sdk.**{ *;}
-keep public class org.**{ *;}

-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-keep class com.onekosmos.fido2authenticator.** {*;}
-keep class com.google.firebase.** { *; }

# For using GSON @Expose annotation
-keepattributes *Annotation*

-keepattributes Signature
-keep class com.google.gson.reflect.TypeToken { *; }

-keep public class com.google.gson.** {*;}
-dontwarn com.google.gson.**

-keep class * implements com.google.gson.** {*;}
-keep class com.google.gson.stream.** { *; }

# Reflection on classes from native code
-keep class com.google.gson.JsonArray { *; }
-keep class com.google.gson.JsonElement { *; }
-keep class com.google.gson.JsonObject { *; }
-keep class com.google.gson.JsonPrimitive { *; }
-dontnote com.google.gson.**