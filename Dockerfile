# =============================================================================
# Etapa 1 — Compilación
#
# Las dependencias se resuelven antes de copiar el código fuente: mientras
# build.gradle no cambie, Docker reutiliza esa capa y la reconstrucción tras
# editar una clase no vuelve a descargar nada.
# =============================================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
RUN chmod +x ./gradlew && ./gradlew dependencies --no-daemon --quiet || true

COPY src ./src

# Los tests de integración levantan contenedores con Testcontainers y no pueden
# correr dentro de la propia construcción de la imagen: se ejecutan en el
# pipeline de CI, antes de llegar aquí.
RUN ./gradlew bootJar --no-daemon -x test

# =============================================================================
# Etapa 2 — Ejecución
#
# Sólo el JRE y el artefacto. La imagen final no contiene Gradle, el código
# fuente ni el JDK.
# =============================================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# Usuario sin privilegios: un proceso comprometido no debe correr como root.
RUN addgroup --system franchise && adduser --system --ingroup franchise franchise

WORKDIR /app
COPY --from=builder --chown=franchise:franchise /build/build/libs/*.jar app.jar

USER franchise

EXPOSE 8080

# MaxRAMPercentage deja que la JVM dimensione el heap según el límite de
# memoria del contenedor en lugar de según la RAM del anfitrión.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport"

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --quiet --spider http://localhost:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
