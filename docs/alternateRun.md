# Other ways to run the CLI

[Conventions]: ../README.md#conventions
[_unreleased snapshot_]: #using-unreleased-snapshots
[java_install]: https://adoptium.net/installation/

## Using unreleased snapshots

Folks picking up early snapshots is really helpful for me, but _using an unreleased snapshot may be unstable_. 
- 🚧 Do not run an unstable CLI directly against notes in your Obsidian vault
- 👷‍♀️ Be prepared to report issues if you find them.
  - Be as specific as you can about the configuration and sources that are not working. 
  - `ttrpg-convert --version` will tell you the version you are running, including the commit! Please include that information in your report.

I recommend staying with official releases unless you are willing to help me debug issues (and your help is very much appreciated!).

## Use JBang

JBang is a tool designed to simplify Java application execution. By eliminating the need for traditional build tools and app servers, JBang enables quick and easy running of Java apps, scripts, and more.

1. Install JBang: https://www.jbang.dev/documentation/guide/latest/installation.html

2. Install the pre-built release of ttrpg-convert-cli: 

    ```shell
    jbang app install --name ttrpg-convert --force --fresh https://github.com/ebullient/ttrpg-convert-cli/releases/download/2.2.15/ttrpg-convert-cli-2.2.15-runner.jar
    ```

    🚧 If you want the latest [_unreleased snapshot_][]: 

    ```shell
    jbang app install --name ttrpg-convert --force --fresh https://github.com/ebullient/ttrpg-convert-cli/releases/download/299-SNAPSHOT/ttrpg-convert-cli-299-SNAPSHOT-runner.jar
    ```

    > 🔹 Feel free to use an alternate alias by replacing the value specified as the name. 
    > For example, for the snapshot, you can use `--name ttrpg-convert-ss`, allowing you to keep both versions available. 
    > You will need to adjust commands accordingly. 

3. Verify the install by running the command: 

    ```shell
    ttrpg-convert --help
    ```

Continue to notes about [Conventions][].

## Use Java to run the jar

To run the CLI, you will need to have **Java 17** installed on your system.

1. Ensure you have [**Java 17** installed on your system][java_install] and active in your path.

2. Download the CLI as a jar

    - Latest release: [ttrpg-convert-cli-2.2.15-runner.jar](https://github.com/ebullient/ttrpg-convert-cli/releases/download/2.2.15/ttrpg-convert-cli-2.2.15-runner.jar)
    - 🚧 [_unreleased snapshot_][]: [ttrpg-convert-cli-299-SNAPSHOT-runner.jar](https://github.com/ebullient/ttrpg-convert-cli/releases/download/299-SNAPSHOT/ttrpg-convert-cli-299-SNAPSHOT-runner.jar)

3. Verify the install by running the command: 

    ```shell
    java -jar ttrpg-convert-cli-2.2.15-runner.jar --help
    ```

    🚧 If you are using the [_unreleased snapshot_][], use the following command:
    ```shell
    java -jar ttrpg-convert-cli-299-SNAPSHOT-runner.jar --help
    ```

To run commands, replace `ttrpg-convert` with `java -jar ttrpg-convert-cli-...`

Continue to notes about [Conventions][].

## Build and run from source

This is a Quarkus project that uses Maven as its build tool.
- You can use the [Quarkus CLI](https://quarkus.io/guides/cli-tooling) to build and run the project
- You can use Maven to build and run the project via the [maven wrapper](https://maven.apache.org/wrapper/) (the `mvnw` script). The Maven Wrapper is a tool that provides a standardized way to execute Maven builds, ensuring the correct version and configurations are used.

1. Clone this repository

2. Ensure you have [**Java 17** installed on your system][java_install] and active in your path.

3. Build this project:
    - Build with the Quarkus CLI: `quarkus build`
    - Build with the Maven wrapper: `./mvnw install`

4. Verify the build: `java -jar target/ttrpg-convert-cli-299-SNAPSHOT-runner.jar --help`

To run commands, either: 

- Replace `ttrpg-convert` with `java -jar target/ttrpg-convert-cli-299-SNAPSHOT-runner.jar`, or
- Use JBang to create an alias that points to the built jar: 

    ```shell
    jbang app install --name ttrpg-convert --force --fresh ~/.m2/repository/dev/ebullient/ttrpg-convert-cli/299-SNAPSHOT/ttrpg-convert-cli-299-SNAPSHOT-runner.jar
    ```

    > 🔹 Use an alternate alias by replacing the value specified as the name: `--name ttrpg-convert`, and adjust the commands accordingly.

Continue to notes about [Conventions][].
