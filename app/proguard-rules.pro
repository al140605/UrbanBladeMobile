# Reglas de R8 para la variante release (ver buildTypes.release en build.gradle.kts).
# Retrofit, OkHttp, Stripe, Firebase, Coil y Media3 traen sus propias reglas de consumidor
# dentro de sus AAR; aquí solo va lo que es específico de esta app.

# Gson llena estos DTO por reflexión, y Retrofit los resuelve por el tipo genérico de cada
# endpoint (UrbanBladeApi). Si R8 renombra o recorta sus campos, el JSON deja de mapearse y
# la app falla solo en release, sin que ni el CI ni el debug lo noten.
-keep class com.urbanblade.mobile.data.model.** { *; }

# Firma de tipos genéricos y anotaciones: Retrofit/Gson las leen en tiempo de ejecución.
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, AnnotationDefault

# Trazas legibles en Crashlytics (el plugin sube el mapping.txt de cada build release).
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
