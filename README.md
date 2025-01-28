# Riviera DEV website

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Prerequisites

- Install a JDK
    - Archive: https://jdk.java.net/23/
    - Brew: https://formulae.brew.sh/formula/openjdk
    - SDKMan: https://sdkman.io/jdks/#open
- Install a container system (any of the two)
    - Docker: https://docs.docker.com/engine/install/
    - Podman: https://podman.io/docs/installation

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

### Fetching production data

By default, at startup, this will create a user with username: 'user', password: 'user' and fetch the
production data and store it in a postgres dev service database started automatically (this
will be started in a container in either docker or podman).

The default production data source can be configured in `src/main/resources/application.properties`:

```xml
%dev.dev-auto-setup.url=https://2024.rivieradev.fr/Serialiser/json
```

You can change this to point to other years such as 2025. This will not fetch users, nor speaker emails,
for confidentiality reasons. This only fetches data which is already public on our website.

## Packaging for production

The application can be compiled natively using:
```shell script
./mvnw -Dnative package
```

Now you can create a Debian package with:
```shell script
fakeroot ./debian/rules clean binary
```

And deploy the resulting Debian package on the production server.

## Related Guides

- Renarde ([guide](https://quarkiverse.github.io/quarkiverse-docs/quarkus-renarde/dev/index.html)): Renarde is a server-side Web Framework based on Quarkus, Qute, Hibernate and RESTEasy Reactive.
- Hibernate ORM with Panache ([guide](https://quarkus.io/guides/hibernate-orm-panache)): Simplify your persistence code for Hibernate ORM via the active record or the repository pattern

## TODO

- On prod, upgrade from postgres 9 to 14 (2024 is on 14, the rest is on 9)
- On dev, test and document db transporter for new users
- Figure out how to write db migrations
