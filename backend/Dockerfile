# =============================================================================
# Backend — Java 21 + Spring Boot
# =============================================================================

# --- Etapa 1: compilar --------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /origen

# Los pom.xml primero y en su propia capa: mientras no cambien, Docker reutiliza
# las dependencias ya descargadas. Sin esto, cada cambio de una linea de codigo
# volveria a bajar internet entero.
#
# ⚠️ AL AÑADIR UN MODULO NUEVO, AÑADIRLO TAMBIEN AQUI.
# Olvidarlo hace que la imagen falle con "Child module X does not exist" — y solo
# al construir en el servidor, porque en local Maven lee el disco directamente.
COPY pom.xml .
COPY shared/pom.xml shared/
COPY identity/pom.xml identity/
COPY organizations/pom.xml organizations/
COPY servers/pom.xml servers/
COPY voting/pom.xml voting/
COPY trust/pom.xml trust/
COPY ranking/pom.xml ranking/
COPY delivery/pom.xml delivery/
COPY billing/pom.xml billing/
COPY app/pom.xml app/

# Comprobacion de coherencia: si el pom padre declara un modulo que no se copio
# arriba, se falla AQUI con un mensaje claro en vez de con un error de Maven
# tres pasos despues.
RUN for m in $(grep -oP '(?<=<module>)[^<]+' pom.xml); do \
      test -f "$m/pom.xml" || { echo "FALTA COPY $m/pom.xml en el Dockerfile"; exit 1; }; \
    done

RUN mvn -B -q dependency:go-offline

COPY . .
RUN mvn -B -DskipTests package

# --- Etapa 2: ejecutar --------------------------------------------------------
# Solo el runtime, sin Maven ni codigo fuente: menos peso y muchisima menos
# superficie de ataque.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Usuario sin privilegios. Si alguien consigue ejecutar codigo dentro del
# contenedor, no lo hace como root.
#
# El UID va FIJO a proposito. Sin -u, Alpine elige el primer hueco libre, que
# puede cambiar entre reconstrucciones de la imagen: la carpeta data/archivos
# del host se queda con el dueño antiguo y las subidas empiezan a fallar con
# "no se puede escribir" sin que nada mas haya cambiado. Con un UID fijo, el
# chown del host se hace una vez y vale para siempre.
#
# ⚠️ Este numero aparece tambien en scripts/actualizar.sh. Si cambia aqui,
# cambia alli.
RUN addgroup -S -g 10001 latinosmc && adduser -S -u 10001 -G latinosmc latinosmc

COPY --from=build /origen/app/target/app-*.jar app.jar
RUN chown -R latinosmc:latinosmc /app
USER latinosmc

EXPOSE 8080

# MaxRAMPercentage en vez de -Xmx fijo: la JVM se adapta al limite de memoria
# que Docker le imponga, sin tener que recalcularlo a mano en cada despliegue.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC"

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
