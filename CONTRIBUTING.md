# Contributing

I am always thrilled to receive pull requests, and I do my best to process them as fast as possible. Not sure if that typo is worth a pull request? Do it! I appreciate it.

If your pull request is not accepted on the first try, don't be discouraged. We can work together to improve the PR so it can be accepted.

## Legal

All original contributions are licensed under the ASL - Apache License, version 2.0 or later, or, if another license is specified as governing the file or directory being modified, such other license.

All contributions are subject to the [Developer Certificate of Origin (DCO)](https://developercertificate.org/). The DCO text is also included verbatim in [dco.txt](dco.txt).

## Please open an issue first

Take a moment to check that an issue doesn't already exist for the behavior or output you're seeing.
If it does, please add a quick thumbs up reaction.
This will help prioritize the most common problems and requests.

If there isn't an issue yet, open one! Be as specific as you can, and include the tool version.

## Build and test the CLI

[Install Java](https://adoptium.net/installation/). This project requires Java 17.

- **Use maven:** `./mvnw install`
- **Use the Quarkus CLI**: `quarkus build`

To test with actual/live data, clone 5eTools and/or PF2eTools into a `sources` directory.

Using the GitHub CLI:

```shell
mkdir -p sources
# 5eTools
gh repo clone 5etools-mirror-1/5etools-mirror-1.github.io sources/5etools-mirror-1.github.io -- --depth=1
gh repo clone TheGiddyLimit/homebrew sources/5e-homebrew -- --depth=1
gh repo clone TheGiddyLimit/unearthed-arcana sources/5e-unearthed-arcana -- --depth=1
# PF2eTools
gh repo clone Pf2eToolsOrg/Pf2eTools sources/Pf2eTools -- --depth=1
```

### Building native images

A Pull Request build will automatically build a native image for Linux. Building a native image requires a few more steps (GraalVM, native-image, etc). See the [Quarkus documentation](https://quarkus.io/guides/building-native-image) for more information.

### Build CSS only

- **build css**: `./mvnw sass-cli:run`
- **watch**: `./mvnw sass-cli:watch`
- **package**: `./mvnw sass-cli:run -Dsass.watch`

## Running tests in the IDE

I'll just talk about VS Code here. IntelliJ has similar features.

To run tests, you need to have live data in the `sources` directory as described above.

1. Install and enable the following extensions:
    - Language support for Java
    - Debugger for Java
    - Test Runner for Java

    Installing these projects will add a few icons to the activity bar on the left:
        - A beaker: that's the Test Explorer view.
        - A play button with a bug: that's the Run/Debug view.

2. Open the Test Explorer view. You will see a hierarchical view of all the tests in the project.

    - You can run individual tests, or run all the tests in a package or class.
    - Tests of note:
        - `dev.ebullient.convert.Tools5eDataConvertTest`: this tests runs against a launch of the CLI. There are different methods here for different input parameters (SRD-only, constrained input, all input, collection of homebrew).
        - `dev.ebullient.convert.tools.dnd5e.*`: This is a collection of tests (`JsonDataNoneTest` is SRD-only, `JsonDataSubsetTest` uses a subset of the data, `JsonDataTest` uses all the data) that all share a common set of test cases defined here: `src/test/java/dev/ebullient/convert/tools/dnd5e/CommonDataTests.java`. There is a method for each type of data, which makes it a bit easier to debug how a certain type is rendered.

    Notes:
        - You will never run \*IT tests from the Test Explorer. These are run by the maven build against the constructed final artifact.

## Coding standards and Conventions

There are two tasks in the Maven build that will format code and sort imports to reduce whitespace/ordering churn in PRs and commits.
Make sure to run `./mvnw package` or `./mvnw process-sources` before you commit, and everything will be formatted correctly.

This project also has a `.editorconfig` file that defines expected behavior for whitespace and line endings.
Most IDEs have an editorconfig plugin that will automatically format your code to match these settings.

### IDE Config and Code Style

If you want your IDE to format your code for you, use the files in the `src/ide-config` directory.

#### Eclipse Setup

Open the *Preferences* window, and then navigate to *Java* -> *Code Style* -> *Formatter*.

- Click *Import* and then select `src/ide-config/eclipse-format.xml`.
- Choose the `ttrpg-convert-cli` profile.

Next navigate to *Java* -> *Code Style* -> *Organize Imports*. Click *Import* and select `src/ide-config/eclipse.importorder`.

#### VS Code

1. Install and enable the "Language Support for Java by Red Hat" extension.
2. Open settings (use the gear icon in the lower left corner of the window), and click the "Workspace" link in the header under the search box to open the workspace settings.
3. Type `eclipse` in the search box, you should see two: `Java > Format: Eclipse` and `Java > Format: Eclipse Export Profile`.
4. Set *Java* -> *Format* -> *Settings URL* to `https://raw.githubusercontent.com/ebullient/ttrpg-convert-cli/main/src/ide-config/eclipse-format.xml`
5. Set *Java* -> *Format* -> *Profile* to `ttrpg-convert-cli`
6. Click "Edit in settings.json" under *Java* -> *Completion* -> *Import Order* and paste the following into the `java.format.imports.order` setting:

    ```json
    "java.completion.importOrder": [
      "#",
      "java",
      "javax",
      "jakarta",
      "org",
      "com",
      ""
    ],
    ```

## Notes on control flow

- `src/main/java/dev/ebullient/convert/RpgDataConvertCli.java` is the entry point for the CLI.
- `src/main/java/dev/ebullient/convert/config` has classes for handling config
- `src/main/java/dev/ebullient/convert/io` has classes for handling input and output.
    - `MarkdownDoclet` renders Javadoc in \*.qute.\* packages.
    - `MarkdownWriter` writes markdown files using configured templates.
    - `Tui` is the "text user interface" and deals with writing things to the console.
- `src/main/java/dev/ebullient/convert/qute` has base/common model/POJO[^1] classes for rendering templates
- `src/main/java/dev/ebullient/convert/tools` has the main logic for converting data
    - There are some common interfaces and classes for handling data, and then there are subpackages for each tool.

[^1]: Plain Old Java Object

### Unconventional conventions

This project is parsing JSON built for JavaScript that has a lot of Union types and other things that are not easily represented in Java. Some of the typical kinds of adapters are not used here: it's too much magic that becomes too hard to read.

This project does use Jackson, but sticks with raw types (`JsonNode`, `ArrayNode`, `ObjectNode`) to a large extent. There are some cases where direct mapping to an object is used, but it's not the norm.

As happens with many projects, I've somewhat changed my approach to things over time. When I first started, it was a lot of strings, which lead to a lot of bugs. When working with Pf2e, I stumbled on an approach that is cleaner, but definitely non-standard.

Flow goes something like this:

- `Index`: CLI is launched. Index is created (`Tools5eIndex` or `Pf2eIndex`). Files are read, with json data parsed and indexed as `JsonNode` data.
- `Prepare`: After all data is read, it is "prepared" (for example, fix missing linkages between types after everything has been indexed)
- `Render`: Selected types are then converted to Markdown using `Json2\*` classes. These classes parse the type-specific data, and construct a `Qute` model used when rendering templates.

Parsing is based on a hierarchy of interfaces with default methods

- 5e: `JsonTextConverter` <- `JsonTextReplacement` <- `JsonSource` <- `Json2QuteCommon` <- `Json2QuteBackground`
- Pf2e: `JsonTextConverter` <- `JsonTextReplacement` <- `JsonSource`<- `Pf2eTypeReader` <- `Json2QuteBase` <- `Json2QuteBackground`

Fields in these Json objects are also read through an enum type hierarchy. These enums allow fields to be defined and typed once (no finger checks) without requiring strict object typing (given union types). This is the preferred practice (for this project), but is not uniformly followed on the (older) 5e side. As I go through things, I'm trying to clean up the 5e side to match the Pf2e side. It's a bit of a mess, but it's a mess that's getting better.
