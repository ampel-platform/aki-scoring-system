FROM eclipse-temurin:21-jre-alpine@sha256:6f78a61a2aa1e6907dda2da3eb791d44ef3d18e36aee1d1bdaa3543bd44cff4b

# Other labels will be set by the CI pipeline
LABEL org.opencontainers.image.title="AKI Scoring System"
LABEL org.opencontainers.image.description="Scoring system for the AMPEL platform related to acute kidney injury (AKI)."
LABEL org.opencontainers.image.authors="AMPEL CDSS Team <ampel@medizin.uni-leipzig.de>"
LABEL org.opencontainers.image.vendor="AMPEL CDSS"

WORKDIR /opt/app
COPY ./target/*jar-with-dependencies.jar ./aki-service.jar

RUN addgroup --system ampel && adduser --system ampel --ingroup ampel
RUN chown -R ampel:ampel /opt/app/

USER ampel:ampel

ENTRYPOINT ["java", "-jar", "/opt/app/aki-service.jar"]
