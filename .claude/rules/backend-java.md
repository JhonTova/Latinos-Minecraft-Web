---
paths:
  - "backend/**/*.java"
---

# Código Java del backend

Se carga al leer o editar cualquier `.java` de `backend/`. Lo general está en
`backend/AGENTS.md`; aquí están las normas que más se incumplen al escribir.

- **Fronteras entre módulos.** Un módulo de dominio (`identity`,
  `organizations`, `servers`, `voting`, `trust`, `ranking`, `delivery`,
  `billing`) no importa clases de otro. Se comunican por eventos o por las
  interfaces de `com.latinosmc.shared.ports`. Solo `app` conoce a todos. Si
  `ArquitecturaTest` falla, cambia el diseño, no la prueba.
- **El tiempo se inyecta.** Recibe un `java.time.Clock` por constructor y usa
  `clock.instant()`. No escribas `Instant.now()`, `LocalDate.now()` ni
  `System.currentTimeMillis()` en lógica de negocio: una prueba no puede
  controlarlos.
- **Errores de negocio con `DomainException`**, no con excepciones genéricas ni
  con códigos de retorno.
- **SQL con parámetros nombrados** (`JdbcClient` y `.param(...)`). Nunca
  concatenes un valor en una consulta. Una lectura sobre una tabla de otro
  módulo solo es válida si está inventariada en ADR-0019; escribir en tablas
  ajenas está prohibido.
- **Ledger inmutable.** Ni `UPDATE` ni `DELETE` sobre `ledger_entries`.
- **Comandos de Minecraft**: lista blanca y parámetros tipados (ADR-0012);
  nunca se arma un comando concatenando texto que viene de fuera.
- **Idioma**: sigue el de las clases vecinas del paquete (`CastVoteService`
  convive con `CodigoDeVoto`). Los comentarios van en español y explican el
  porqué.
- **Formato**: imita el del archivo que editas. No ejecutes
  `mvn spotless:apply`.
- **No agregues dependencias** a ningún `pom.xml` sin que el desarrollador lo
  decida; si se aprueba, se registra en un ADR.
