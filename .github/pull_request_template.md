## Spec y tareas
<!-- Spec (A-00x) y tareas (T-A00x-yy) que implementa este PR -->

## Cambios
<!-- Qué cambia y por qué -->

## Pruebas
<!-- Tests añadidos o actualizados; cómo se verificó en el dispositivo si procede -->

## Checklist (Definition of Done)
- [ ] `./gradlew verifyArchitecture spotlessCheck test testDebugUnitTest assembleDebug` en verde
- [ ] `:domain` sigue sin importar nada de Android, DI, red ni persistencia
- [ ] Los ViewModels no reciben `Context`, `Activity` ni `Application`
- [ ] Cámara y permisos solo en la capa `ui`
- [ ] Textos en `strings.xml` y `contentDescription` correctos
- [ ] Cada composable nuevo tiene `@Preview`
- [ ] Contratos compartidos actualizados en ambos repositorios (si cambian)
