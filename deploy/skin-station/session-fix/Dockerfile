FROM golang:1.25-alpine AS build
RUN apk add --no-cache build-base gettext git
WORKDIR /src
COPY source/ ./
RUN go test -p 2 -run 'TestDuskRainStaleValidateRefresh|TestAuth' -count=1 ./
RUN CGO_ENABLED=1 go build -p 2 -ldflags '-linkmode external -extldflags "-static"' -o /out/drasl .
FROM unmojang/drasl@sha256:5646d68dc458431d6c2a4a757b77472d50e3881eeac51f68f76d88cbb7a13b37
COPY --from=build /out/drasl /opt/duskrain-session-fix/drasl
CMD ["/opt/duskrain-session-fix/drasl"]
