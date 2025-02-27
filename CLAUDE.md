# WaveReader Development Guidelines

## Build Commands
- Build: `./gradlew build`
- Clean build: `./gradlew clean build`
- Install debug APK: `./gradlew installDebug`
- Run unit tests: `./gradlew test`
- Run single test: `./gradlew test --tests "com.github.b4ndithelps.wave.TestClass.testMethod"`
- Run instrumented tests: `./gradlew connectedAndroidTest`

## Code Style Guidelines
- **Package Structure**: Follow `com.github.b4ndithelps.wave.[feature]` pattern
- **Naming Conventions**: 
  - Classes: PascalCase (e.g., `BookData`)
  - Variables/Methods: camelCase (e.g., `bookPath`)
  - Constants: UPPER_SNAKE_CASE
- **Kotlin Style**: Use data classes for models, prefer extension functions
- **Room Database**: Use suspension functions with DAO interfaces
- **Error Handling**: Use result wrappers or coroutines' exception handling
- **UI Components**: Follow Material Design guidelines with custom theming
- **Imports**: Organize by standard > androidx > third-party