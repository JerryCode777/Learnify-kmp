# Learnify - Arquitectura del Proyecto

## 🏗️ Stack Tecnológico

- **Kotlin Multiplatform** - Compartir código entre Android e iOS
- **Compose Multiplatform** - UI declarativa multiplataforma
- **SQLDelight** - Base de datos local type-safe
- **Ktor Client** - Cliente HTTP para Gemini API
- **Koin** - Inyección de dependencias
- **Kotlinx Serialization** - Serialización JSON
- **Napier** - Logging multiplataforma

## 📁 Estructura del Proyecto

```
composeApp/src/commonMain/kotlin/org/example/learnify/
├── domain/
│   ├── model/           # Entidades del negocio
│   │   ├── Document.kt
│   │   ├── LearningPath.kt
│   │   └── Quiz.kt
│   ├── repository/      # Interfaces de repositorios
│   └── usecase/         # Casos de uso (lógica de negocio)
│
├── data/
│   ├── local/           # Implementación de base de datos local
│   ├── remote/          # Cliente API (Gemini)
│   │   └── GeminiApiClient.kt
│   └── repository/      # Implementación de repositorios
│
├── presentation/
│   ├── upload/          # Pantalla de subida de documentos
│   ├── learning_path/   # Pantalla de ruta de aprendizaje
│   ├── quiz/            # Pantalla de quizzes
│   └── components/      # Componentes UI reutilizables
│
├── di/                  # Módulos de Koin
│   └── AppModule.kt
│
└── util/                # Utilidades compartidas

composeApp/src/commonMain/sqldelight/
└── org/example/learnify/database/
    └── Document.sq      # Schema y queries de SQLDelight
```

## 🔄 Flujo de la Aplicación

1. **Upload de Documento**
   - Usuario sube PDF
   - Extracción de texto del PDF (específico por plataforma)
   - Guardado en base de datos local

2. **Generación de Ruta de Aprendizaje**
   - Envío de contenido a Gemini API
   - Procesamiento de respuesta
   - Organización en topics
   - Guardado en SQLDelight

3. **Visualización de Contenido**
   - Lectura desde SQLDelight
   - Mostrar teoría por topic
   - Navegación entre topics

4. **Quizzes Interactivos**
   - Generación de preguntas con Gemini
   - Presentación al usuario
   - Guardado de resultados
   - Tracking de progreso

## 🎯 Principios de Arquitectura

- **Clean Architecture**: Separación en capas (domain, data, presentation)
- **Single Source of Truth**: SQLDelight como fuente única de verdad
- **Unidirectional Data Flow**: Estado fluye de data → presentation
- **Dependency Injection**: Koin para gestión de dependencias
- **Multiplataforma**: Código compartido maximizado en commonMain

## 🔐 Consideraciones de Seguridad

- API key de Gemini debe estar en storage seguro (no hardcoded)
- Datos sensibles encriptados en SQLDelight
- Validación de entrada del usuario

## 📱 Plataformas Soportadas

- ✅ Android (minSdk 24)
- ✅ iOS (iOS 14+)

## 🚀 Próximos Pasos

1. ✅ Configurar dependencias
2. ⏳ Implementar DatabaseDriver (Android/iOS specific)
3. ⏳ Crear repositorios y use cases
4. ⏳ Implementar UI screens
5. ⏳ Integración con Gemini API
6. ⏳ Testing y polish

## 📝 Notas

- El procesador de PDFs usa Gemini 3 Flash para extracción de contenido
- La app genera rutas de aprendizaje personalizadas
- Todo el procesamiento se guarda localmente para uso offline
