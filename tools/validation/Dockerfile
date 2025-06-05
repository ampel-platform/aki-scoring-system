FROM eclipse-temurin:21-jre-alpine@sha256:6f78a61a2aa1e6907dda2da3eb791d44ef3d18e36aee1d1bdaa3543bd44cff4b

WORKDIR /opt/app
COPY ./target/*jar-with-dependencies.jar ./aki-validation.jar

RUN addgroup --system ampel && adduser --system ampel --ingroup ampel
RUN chown -R ampel:ampel /opt/app/

USER ampel:ampel

ENTRYPOINT ["java", "-jar", "/opt/app/aki-validation.jar"]
