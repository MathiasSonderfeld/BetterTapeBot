from gcr.io/distroless/java25-debian13:nonroot
COPY build/libs/app.jar /app.jar
CMD ["/app.jar"]