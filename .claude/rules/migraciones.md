---
paths:
  - "backend/app/src/main/resources/db/migration/**"
---

# Migraciones Flyway

Se carga al leer o editar una migración.

- **Una migración que ya está en `main` no se edita ni se borra**: ya se aplicó
  en producción y Flyway compara su checksum al arrancar. Para corregir algo se
  crea otra migración.
- La nueva lleva el siguiente número libre en orden numérico (`V10` va después
  de `V9`), sin huecos ni números repetidos.
- Antes de escribir una, carga la skill `migracion-flyway`: tiene el formato del
  nombre, el encabezado y las reglas de las columnas.
- Una migración es una **parada obligatoria**: el desarrollador la aprueba antes
  de escribirla y el pull request la declara en la primera línea.
