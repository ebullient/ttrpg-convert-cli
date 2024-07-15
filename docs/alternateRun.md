# Other ways to run the CLI

- [Use JBang](#use-jbang)
- [Use Homebrew](#use-homebrew)
    - [Use pre-built platform binary](#use-pre-built-platform-binary)
- [Use Java to run the jar](#use-java-to-run-the-jar)
- [Build and run from source](#build-and-run-from-source)
- [Using unreleased snapshots](#using-unreleased-snapshots)

[conventions]: ../README.md#conventions
[5etools-data]: ../README.md#convert-5etools-json-data
[pf2e-data]: ../README.md#convert-pf2etools-json-data
[homebrew]: ../README.md#convert-homebrew-json-data
[config]: ./configuration.md
[_unreleased snapshot_]: #using-unreleased-snapshots
[java_install]: https://adoptium.net/installation/

## Use JBang

JBang is a tool designed to simplify Java application execution. By eliminating the need for traditional build tools and app servers, JBang enables quick and easy running of Java apps, scripts, and more.

1. Install JBang: <https://www.jbang.dev/documentation/guide/latest/installation.html>

2. Install the pre-built release of ttrpg-convert-cli:

    ```shell
    jbang app install --name ttrpg-convert --force --fresh https://github.com/ebullient/ttrpg-convert-cli/releases/download/2.3.17/ttrpg-convert-cli-2.3.17-runner.jar
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

    Notice there is no leading `./` or `.\`. JBang installs the command in a location that is on your PATH[^1].

Next steps:

- Understand [CLI plugin and usage conventions][conventions].
- [Convert 5eTools JSON data][5etools-data]
- [Convert PF2eTools JSON data][pf2e-data]
- [Convert Homebrew JSON data][homebrew]
- Create your own [configuration file][config].

## Use Homebrew

Not to be confused with Homebrew adventures, Homebrew is a package manager for Mac OS (and sometimes linux).

1. Install Homebrew: <https://brew.sh/>
2. Install the `tap`:

    ```shell
    brew tap ebullient/tap
    ```

3. Install the cli:

    ```shell
    brew install ttrpg-convert-cli
    ```

4. Verify the install by running the command (from anywhere):

    ```shell
    ttrpg-convert --help
    ```

    Notice there is no leading `./` or `.\`. Homebrew installs the command in a location that is on your PATH[^1].

Next steps:

- Understand [CLI plugin and usage conventions][conventions].
- [Convert 5eTools JSON data][5etools-data]
- [Convert PF2eTools JSON data][pf2e-data]
- [Convert Homebrew JSON data][homebrew]
- Create your own [configuration file][config].

### Use pre-built platform binary

> [!NOTE]
> 📝 *Where do these binaries come from?*
>
> They are built on GitHub managed CI runners using the workflow defined [here](https://github.com/ebullient/ttrpg-convert-cli/blob/main/.github/workflows/release.yml), which compiles a Quarkus application (Java) into a platform-native binary using [GraalVM](https://www.graalvm.org/). I build and upload the mac arm64 binary myself (not supported by GH CI) using [this script](https://github.com/ebullient/ttrpg-convert-cli/blob/main/.github/augment-release.sh).

[Download the latest release](https://github.com/ebullient/ttrpg-convert-cli/releases/latest) of the zip or tgz for your platform. Extract the archive. A `ttrpg-convert` binary executable will be in the extracted bin directory.

In a terminal or command shell, navigate to the directory where you extracted the archive and run the command in the `bin` directory:

```shell
# Linux or MacOS (use the leading ./ because the current directory is not in the PATH[^1])
./ttrpg-convert --help

# Windows (the .exe extension is optional)
ttrpg-convert.exe --help
```

Notes:

- Windows users: the `.exe` extension is optional. You can run `ttrpg-convert.exe` or `ttrpg-convert` interchangeably.
- Folks familar with command line tools can add the `bin` directory to their path to make the command available from anywhere.
- _MacOS permission checking_ (unverified executable): `xattr -r -d com.apple.quarantine <path/to>/ttrpg-convert`

Next steps:

- Understand [CLI plugin and usage conventions][conventions].
- [Convert 5eTools JSON data][5etools-data]
- [Convert PF2eTools JSON data][pf2e-data]
- [Convert Homebrew JSON data][homebrew]
- Create your own [configuration file][config].

## Use Java to run the jar

To run the CLI, you will need to have **Java 17** installed on your system.

1. Ensure you have [**Java 17** installed on your system][java_install] and active in your path.

2. Download the CLI as a jar

    - Latest release: [ttrpg-convert-cli-2.3.17-runner.jar](https://github.com/ebullient/ttrpg-convert-cli/releases/download/2.3.17/ttrpg-convert-cli-2.3.17-runner.jar)
    - 🚧 [_unreleased snapshot_][]: [ttrpg-convert-cli-299-SNAPSHOT-runner.jar](https://github.com/ebullient/ttrpg-convert-cli/releases/download/299-SNAPSHOT/ttrpg-convert-cli-299-SNAPSHOT-runner.jar)

3. Verify the install by running the command:

    ```shell
    java -jar ttrpg-convert-cli-2.3.17-runner.jar --help
    ```

    🚧 If you are using the [_unreleased snapshot_][], use the following command:

    ```shell
    java -jar ttrpg-convert-cli-299-SNAPSHOT-runner.jar --help
    ```

To run commands, replace `ttrpg-convert` with `java -jar ttrpg-convert-cli-...`

Next steps:

- Understand [CLI plugin and usage conventions][conventions].
- [Convert 5eTools JSON data][5etools-data]
- [Convert PF2eTools JSON data][pf2e-data]
- [Convert Homebrew JSON data][homebrew]
- Create your own [configuration file][config].

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

Next steps:

- Understand [CLI plugin and usage conventions][conventions].
- [Convert 5eTools JSON data][5etools-data]
- [Convert PF2eTools JSON data][pf2e-data]
- [Convert Homebrew JSON data][homebrew]
- Create your own [configuration file][config].

## Using unreleased snapshots

Folks picking up early snapshots is really helpful for me, but _using an unreleased snapshot may be unstable_.

- [Build from source](#build-and-run-from-source)
- [Use JBang to install the snapshot](#use-jbang)

- 🚧 Do not run an unstable CLI directly against notes in your Obsidian vault
- 👷‍♀️ Be prepared to report issues if you find them.
    - Be as specific as you can about the configuration and sources that are not working.
    - `ttrpg-convert --version` will tell you the version you are running, including the commit! Please include that information in your report.

I recommend staying with official releases unless you are willing to help me debug issues (and your help is very much appreciated!).

[^1]: A PATH is a list of directories that the operating system searches for executables. When you type a command in a terminal, the system looks in each directory in the path for an executable with the name you typed. If it finds one, it runs it. If it doesn't, it reports an error. See [Wikipedia](https://en.wikipedia.org/wiki/PATH_(variable)) for a rough overview and more links.
