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


## Configuration

In the admin part (https://\<url\>/_renarde/backoffice/index) there is a special table `Configurations`. All the keys are in defined in [`ConfigurationKey.java`](./src/main/java/model/ConfigurationKey.java).

Here is how to configure it.

### Launch of the new edition

When we set up the website for the new edition.

-   `GOOGLE_MAP_API_KEY`: Reuse the same key as for the previous edition
-   `EVENT_START_DATE`: Start date of the conference in ISO format. E.g. `2019-05-15T08:20:00`
-   `EVENT_END_DATE`: End date of the conference in ISO format: E.g. `2019-05-17T18:00:00`
-   `DISPLAY_FULL_SCHEDULE = false`: We don't want to display the schedule because we don't know it yet.
-   `DISPLAY_NEW_SPEAKERS = false`: Same as above.
-   `DISPLAY_TALKS = false`: Same as above.
-   `PROMOTED_PAGE`: It's for the primary button shown on the home page below the logo. The possible values are be `CFP`, `TICKETS` or `SPONSORS`. At this time it's a bit tricky because none of them is available 😅 So let's not use it for now.
-   `PROMOTED_PAGE_2`; It's the same as above but for the secondary button. The possible values are `CFP`, `SPONSORS` and `SCHEDULE`. And it's the same, let's not use it for now.
-   `TICKETING_URL`: At this time the ticketing is not opened yet, but if we already know the URL, we can fill it.
-   `TICKETING_OPEN = false`
-   `TICKETING_TRAINING_URL`: To be filled with the URL provided by the training organization, but you probably don't know it yet
-   `TICKETING_TRAINING_OPEN = false` The training organization is not ready yet
-   `SPONSORING_LEAFLET_URL`: URL to the sponsoring leaflet.
-   `CFP_URL`: At this time the CFP is not opened yet, but if we know the URL, we can fill it.
-   `CFP_OPEN = false`

⚠️ `PROMOTED_PAGE` and `PROMOTED_PAGE_2` won't display anything if `EVENT_START_DATE` and `EVENT_END_DATE` are in the past.

## When the sponsoring leaflet is ready

-   `SPONSORING_LEAFLET_URL`: URL to the sponsoring leaflet if not already filled.
-   `PROMOTED_PAGE = 'SPONSORS'`

## When we open the CFP

-   `CFP_URL`: Fill it if it's not already done.
-   `CFP_OPEN = true`
-   `PROMOTED_PAGE = CFP`
-   `PROMOTED_PAGE_2 = SPONSORS` as soon as the leaflet is ready

## When we open the ticketing

-   `TICKETING_URL`: Fill it if it's not already done.
-   `TICKETING_OPEN = true`

Don't forget to fill the tables `PricePacks` and `PricePackDates`.

## When the training organization is ready

-   `TICKETING_TRAINING_URL`: Fill it if it's not already done.
-   `TICKETING_TRAINING_OPEN = true`

## When we close the CFP

-   `CFP_OPEN = false`
-   `PROMOTED_PAGE = TICKETS`

## When some talks and speakers are known

Before changing the configuration, we need to add some talks and speakers.

-   `DISPLAY_TALKS = true`
-   `DISPLAY_NEW_SPEAKERS = true`

## When the full schedule is known

-   `DISPLAY_FULL_SCHEDULE = true`
-   `PROMOTED_PAGE_2 = SCHEDULE`

## When we close the ticketing

-   `TICKETING_OPEN = true` Yes, you read correctly, we don't change the value.
-   Check the checkbox `soldout` in each concerned `PricePacks`
-   `PROMOTED_PAGE = SPONSORS`


## TODO

- On prod, upgrade from postgres 9 to 14 (2024 is on 14, the rest is on 9)
- On dev, test and document db transporter for new users
- Figure out how to write db migrations
