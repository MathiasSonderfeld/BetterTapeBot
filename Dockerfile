ARG BASE_TAG=nonroot
FROM gcr.io/distroless/java25-debian13:${BASE_TAG}
COPY build/libs/app.jar /app.jar
CMD ["/app.jar"]