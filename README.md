# Riviera DEV website

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

This will start by creating a user with username: 'user', password: 'user' and fetch the
production data and store it in a postgres dev service database started automatically.

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
