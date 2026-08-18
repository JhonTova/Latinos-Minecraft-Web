# Requisito 06 — RFC-002: Enterprise Security Program & Zero Trust Architecture

> **Origen:** documento de entrada del usuario
> **Recibido:** 2026-08-03
> **Estado:** guardado, pendiente de ejecutar
> **Tipo:** programa de seguridad — sin implementación

---

## Texto original del prompt (verbatim)

# MINECRAFT PLATFORM
DOCUMENT 06

RFC-002

ENTERPRISE SECURITY PROGRAM
&
ZERO TRUST ARCHITECTURE

VERSION 1.0

================================================================================
OBJETIVO
================================================================================

Este documento define la estrategia completa de seguridad del producto.

NO debes escribir código.

NO debes implementar autenticación.

NO debes crear middleware.

NO debes crear filtros.

NO debes generar configuración.

NO debes escribir reglas de firewall.

Tu única misión consiste en diseñar el programa de seguridad empresarial que protegerá toda la plataforma.

Piensa como un Principal Security Engineer de:

Stripe

Cloudflare

Google

GitHub

AWS

Microsoft

La seguridad NO es un módulo.

Es una capacidad transversal que afecta absolutamente todo el sistema.

================================================================================
FILOSOFÍA
================================================================================

Quiero una plataforma Zero Trust.

Nada se considera confiable.

Ni el navegador.

Ni el plugin.

Ni el frontend.

Ni las APIs.

Ni los administradores.

Ni los servidores.

Toda petición debe verificarse.

Toda acción debe autorizarse.

Todo acceso debe registrarse.

Todo evento importante debe auditarse.

================================================================================
OBJETIVOS
================================================================================

Diseña una estrategia que minimice:

Fraude.

Robo de cuentas.

Votos falsos.

Bots.

Abuso de APIs.

Ataques DDoS.

Escalación de privilegios.

Secuestro de sesiones.

Manipulación de pagos.

Manipulación de subastas.

Ataques internos.

Fugas de información.

================================================================================
ESTÁNDARES
================================================================================

Analiza y justifica la adopción de:

OWASP ASVS

OWASP Top 10

OWASP API Security Top 10

NIST Cybersecurity Framework

CIS Benchmarks

SLSA

Sigstore

CWE

MITRE ATT&CK

STRIDE

LINDDUN (analiza si aporta valor para privacidad)

Explica cuáles deben aplicarse y por qué.

================================================================================
THREAT MODELING
================================================================================

Realiza un Threat Modeling completo.

Utiliza STRIDE.

Analiza cada módulo.

Frontend.

Backend.

Plugin.

API.

Webhooks.

Subastas.

Pagos.

Marketplace.

OAuth.

Analytics.

SDK.

Para cada amenaza define:

Riesgo.

Impacto.

Probabilidad.

Mitigación.

Controles.

================================================================================
IDENTIDAD
================================================================================

Diseña completamente el sistema de identidad.

Usuarios.

Organizaciones.

Máquinas.

Plugins.

SDK.

Bots internos.

Servicios.

Tokens.

Credenciales.

Machine Identity.

================================================================================
AUTENTICACIÓN
================================================================================

Diseña completamente.

OAuth 2.1.

OIDC.

Google.

Discord.

GitHub.

Microsoft.

Email.

Passkeys (WebAuthn).

MFA.

Recovery Codes.

Device Trust.

Magic Links (analiza si conviene).

Explica ventajas y desventajas.

================================================================================
AUTORIZACIÓN
================================================================================

Diseña un modelo moderno.

RBAC.

ABAC.

Policy Based Access Control.

Least Privilege.

Permission Boundaries.

Context Aware Authorization.

Explica cuál utilizar para cada escenario.

================================================================================
GESTIÓN DE SESIONES
================================================================================

Diseña completamente.

Rotación.

Expiración.

Revocación.

Refresh Tokens.

Concurrent Sessions.

Trusted Devices.

Suspicious Login Detection.

Session Fingerprints.

Session Dashboard.

================================================================================
GESTIÓN DE DISPOSITIVOS
================================================================================

Diseña un Device Registry.

Registrar.

Revocar.

Nombrar.

Confiar.

Bloquear.

Notificar.

================================================================================
SECRETS MANAGEMENT
================================================================================

Diseña un sistema profesional.

Secrets.

API Keys.

Encryption Keys.

Rotation.

Vault.

KMS.

No almacenar secretos en texto plano.

================================================================================
CRYPTOGRAPHY
================================================================================

Analiza.

AES-256-GCM.

ChaCha20-Poly1305.

Argon2id.

bcrypt.

Ed25519.

ECDSA.

RSA.

HMAC.

SHA-256.

SHA-3.

UUIDv7.

Explica cuándo utilizar cada uno.

================================================================================
API SECURITY
================================================================================

Diseña completamente.

Rate Limits.

Scopes.

Replay Protection.

Nonce.

Idempotency.

Request Signing.

HMAC.

Timestamp Validation.

API Gateway.

mTLS (analiza si aplica para servicios internos).

================================================================================
PLUGIN SECURITY
================================================================================

El plugin Paper será una pieza crítica.

Diseña completamente.

Autenticación mutua.

Heartbeat firmado.

API Keys.

Rotación.

Versionado.

Revocación.

Protección contra plugins modificados.

Protección contra replay.

Protección contra MITM.

================================================================================
PAGOS
================================================================================

Diseña medidas de seguridad específicas.

Idempotencia.

Ledger.

Firmas.

Verificación de Webhooks.

Replay Protection.

Fraude.

Chargebacks.

Conciliación.

================================================================================
SUBASTAS
================================================================================

Analiza riesgos.

Bots.

Pujas falsas.

Spam.

Auto Bid Manipulation.

Race Conditions.

Timing Attacks.

Collusion.

Fraude entre usuarios.

Explica mitigaciones.

================================================================================
VOTOS
================================================================================

Diseña una estrategia Anti Fraud.

VPN Detection.

Proxy Detection.

Datacenter Detection.

Velocity Checks.

Fingerprint.

Behavior Analysis.

Duplicate Detection.

Risk Score.

Captcha Adaptativo.

Machine Learning (analiza si aporta valor).

Explica completamente.

================================================================================
ABUSE PREVENTION
================================================================================

Diseña una plataforma Anti Abuse.

Spam.

Bots.

Credential Stuffing.

Fake Accounts.

Mass Registration.

Fake Servers.

Malicious Plugins.

API Abuse.

================================================================================
AUDITORÍA
================================================================================

Todo debe quedar registrado.

Login.

Logout.

Password Change.

Role Change.

Payment.

Bid.

Vote.

Server Update.

Permission Change.

API Key Rotation.

Feature Flag Changes.

Todo.

Nunca eliminar registros.

================================================================================
MONITOREO
================================================================================

Diseña un SOC básico.

Alertas.

SIEM.

Anomalías.

Eventos críticos.

Dashboards.

================================================================================
INCIDENT RESPONSE
================================================================================

Diseña un plan profesional.

Detección.

Clasificación.

Contención.

Erradicación.

Recuperación.

Postmortem.

Lecciones aprendidas.

================================================================================
BACKUPS
================================================================================

Seguridad.

Encriptación.

Verificación.

Restauración.

PITR.

Retención.

================================================================================
SUPPLY CHAIN SECURITY
================================================================================

Analiza completamente.

Dependencias.

SBOM.

Firmas.

SLSA.

Dependabot.

Renovate.

Sigstore.

Verificación de artefactos.

================================================================================
PRIVACIDAD
================================================================================

Analiza.

GDPR.

CCPA.

Cookies.

Consentimiento.

Retención.

Derecho al olvido.

Anonimización.

Pseudonimización.

================================================================================
COMPLIANCE
================================================================================

Explica qué certificaciones deberían considerarse en el futuro.

SOC 2.

ISO 27001.

PCI DSS (si aplica).

================================================================================
DIAGRAMAS
================================================================================

Genera.

Trust Boundaries.

Threat Maps.

Authentication Flow.

Authorization Flow.

Sequence Diagrams.

Mermaid.

================================================================================
ADR
================================================================================

Genera Architecture Decision Records.

ADR-030.

Zero Trust.

ADR-031.

Passkeys.

ADR-032.

Vault.

ADR-033.

Argon2id.

ADR-034.

Risk Engine.

ADR-035.

Threat Modeling.

================================================================================
RIESGOS
================================================================================

Analiza riesgos técnicos.

Riesgos organizacionales.

Riesgos humanos.

Riesgos financieros.

Riesgos operativos.

Riesgos de terceros.

Para cada uno define controles preventivos y detectivos.

================================================================================
ENTREGA
================================================================================

NO escribir código.

NO implementar autenticación.

NO crear middleware.

NO generar configuración.

Quiero un Enterprise Security Program completo.

Debe sentirse como la estrategia oficial de seguridad de una empresa SaaS de clase mundial.

Incluye diagramas Mermaid, STRIDE, matrices de riesgo, controles, políticas, ADR y recomendaciones de mejora continua.

Si detectas oportunidades para elevar el nivel de seguridad del producto, intégralas y justifica su adopción.

---

## Notas del CTO sobre este requisito

### Valoración general

Este documento aborda por fin el riesgo que el CTO señaló como **dominante**
en el documento de arranque: el fraude de votos. La sección VOTOS y la
clasificación de la seguridad como capacidad transversal (no módulo) son
correctas.

Elecciones criptográficas correctas: **Argon2id** para contraseñas (no bcrypt
para nuevo desarrollo), **Ed25519** para firmas, **AES-256-GCM** para cifrado
simétrico, **HMAC-SHA256** para webhooks.

### Hallazgo crítico 1 — "Protección contra plugins modificados" es imposible

Esta línea del prompt describe un objetivo inalcanzable, y el error tiene
consecuencias arquitectónicas graves si no se corrige.

**Por qué es imposible:**

1. El JAR del plugin se ejecuta en hardware propiedad del dueño del servidor
2. El dueño del servidor es precisamente quien tiene incentivo económico para hacer trampas
3. Java se decompila trivialmente
4. La API Key vive en el `config.yml` del servidor — se la damos nosotros
5. Un plugin modificado firma datos **falsos** con una clave **legítima**: la firma es válida, el contenido es mentira

Ninguna ofuscación, checksum ni atestación resuelve esto. Es el problema
clásico del cliente no confiable, y en este caso el cliente es adversario.

**Consecuencia arquitectónica obligatoria:**

> El plugin **nunca** puede ser fuente de verdad para nada que afecte al
> ranking, a las recompensas o a la facturación. El plugin es un **actuador**
> ("entrega esta recompensa"), no un **sensor** de confianza.

Reclasificación de la información que reporta el plugin:

| Dato | ¿Confiable? | Tratamiento correcto |
|---|---|---|
| "El jugador X votó" | ❌ Nunca | El voto se origina y valida **en la plataforma**, jamás en el plugin |
| Jugadores conectados | ❌ Autorreportado | Medir con **ping directo** desde nuestra infraestructura al servidor (protocolo de server list ping) — verificable e imposible de falsear desde el plugin |
| TPS, memoria, uptime | ❌ Autorreportado | Solo para el panel del propio dueño. **Nunca** como factor de ranking |
| Versión del plugin | ⚠️ Autorreportado | Aceptable: mentir no da ventaja |
| "Recompensa entregada" | ⚠️ Autorreportado | Confirmación de entrega, no prueba |

**Regla resultante:** todo dato que influya en el ranking debe medirlo la
plataforma por sí misma o derivarse de acciones ocurridas en la plataforma.

### Hallazgo crítico 2 — La detección de VPN/proxy es de pago y además insuficiente

"VPN Detection, Proxy Detection, Datacenter Detection" no son algoritmos que
se programen: son **servicios comerciales de terceros con coste por consulta**
(bases de datos de reputación de IP). Implicaciones:

- Es una **línea de coste operativo permanente**, proporcional al volumen de votos
- Introduce una dependencia externa en la ruta crítica del voto (requiere caché, timeouts y circuit breaker)
- **Los proxies residenciales derrotan esta defensa**, y son exactamente los que usa quien compra votos en serio

**Conclusión:** la reputación de IP es una señal **de apoyo**, nunca la
defensa principal. El antifraude real debe apoyarse en:

1. Análisis de comportamiento (patrones temporales, secuencias imposibles)
2. Análisis de grafo (detección de colusión entre servidores y cuentas)
3. Consistencia de identidad a lo largo del tiempo
4. Captcha adaptativo solo ante señales de riesgo

### Hallazgo crítico 3 — El documento se contradice a sí mismo

| Sección AUDITORÍA | Sección PRIVACIDAD |
|---|---|
| "Todo debe quedar registrado. **Nunca eliminar registros.**" | "**Derecho al olvido.** Anonimización. Pseudonimización." |

Es la tercera vez que aparece "nunca eliminar" (ya estaba en el Documento 04)
y aquí colisiona con una sección del **mismo documento**. La resolución es la
ya propuesta en las notas del Requisito 04: conservar el *hecho* auditado,
pseudonimizar al *sujeto* según política de retención.

### Machine Learning para fraude — veredicto

**No al principio.** Un modelo necesita datos etiquetados (votos confirmados
como fraudulentos) que hoy no existen. La secuencia correcta:

1. Motor de reglas + risk score → funciona desde el día 1, es explicable y auditable
2. Las decisiones del motor de reglas **generan el conjunto de datos etiquetado**
3. ML solo cuando haya volumen suficiente y las reglas muestren límites claros

Ventaja adicional de las reglas: son defendibles ante un cliente que reclama.
"El modelo lo decidió" no es una respuesta aceptable cuando se suspende a un
servidor de pago.

### Compliance — advertencia de coste

SOC 2 e ISO 27001 no son trabajo de ingeniería: son **auditorías externas de
pago anuales** que exigen procesos organizativos documentados, controles de
acceso de personal y evidencias continuas. No tienen sentido hasta que un
cliente empresarial los exija por contrato. PCI-DSS se mantiene fuera de
alcance mientras no se toquen datos de tarjeta (ya decidido: ADR-0004).

### Estándares — priorización recomendada

| Estándar | Prioridad | Motivo |
|---|---|---|
| OWASP ASVS | 🔴 Alta | Checklist accionable de requisitos verificables |
| OWASP API Security Top 10 | 🔴 Alta | Aplica directamente: BOLA es el riesgo nº1 en multitenancy |
| STRIDE | 🔴 Alta | Método de modelado de amenazas, barato y efectivo |
| Dependabot / Renovate + SBOM | 🟡 Media | Automatizable, bajo coste |
| SLSA / Sigstore | 🟢 Baja por ahora | Relevante cuando el plugin se distribuya masivamente |
| NIST CSF / CIS / MITRE ATT&CK | 🟢 Baja | Marcos organizativos, no accionables con un equipo pequeño |
| LINDDUN | 🟡 Media | Sí aporta valor: modela amenazas de **privacidad**, que aquí son reales (menores, IPs) |
