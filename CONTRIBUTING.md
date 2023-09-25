# Contributing

I am always thrilled to receive pull requests, and I do my best to process them as fast as possible. Not sure if that typo is worth a pull request? Do it! I appreciate it.

If your pull request is not accepted on the first try, don't be discouraged. We can work together to improve the PR so it can be accepted.

## Legal

All original contributions are licensed under the ASL - Apache License, version 2.0 or later, or, if another license is specified as governing the file or directory being modified, such other license.

All contributions are subject to the [Developer Certificate of Origin (DCO)](https://developercertificate.org/). The DCO text is also included verbatim in [dco.txt](dco.txt).

## Please open an issue first...

Take a moment to check that an issue doesn't already exist for the behavior or output you're seeing. 
If it does, please add a quick thumbs up reaction. 
This will help prioritize the most common problems and requests.

If there isn't an issue yet, open one! Be as specific as you can, and include the tool version.

## Coding standards and Conventions

There are two tasks in the Maven build that will format code and sort imports to reduce whitespace/ordering churn in PRs and commits.
Make sure to run `./mvnw package` or `./mvnw process-sources` before you commit, and everything will be formatted correctly.

This project also has a `.editorconfig` file that defines expected behavior for whitespace and line endings.
Most IDEs have an editorconfig plugin that will automatically format your code to match these settings.

### IDE Config and Code Style

If you want your IDE to format your code for you, use the files in the `src/ide-config` directory.

#### Eclipse Setup

Open the *Preferences* window, and then navigate to _Java_ -> _Code Style_ -> _Formatter_. 

- Click _Import_ and then select `src/ide-config/eclipse-format.xml`.
- Choose the `ttrpg-convert-cli` profile.

Next navigate to _Java_ -> _Code Style_ -> _Organize Imports_. Click _Import_ and select `src/ide-config/eclipse.importorder`.

### VS Code

1. Install and enable the "Language Support for Java by Red Hat" extension.
2. Open settings (use the gear icon in the lower left corner of the window), and click the "Workspace" link in the header under the search box to open the workspace settings.
3. Type `eclipse` in the search box, you should see two: `Java > Format: Eclipse` and `Java > Format: Eclipse Export Profile`. 
4. Set _Java_ -> _Format_ -> _Settings URL_ to `https://raw.githubusercontent.com/ebullient/ttrpg-convert-cli/main/src/ide-config/eclipse-format.xml`
5. Set _Java_ -> _Format_ -> _Profile_ to `ttrpg-convert-cli`
6. Click "Edit in settings.json" under _Java_ -> _Completion_ -> _Import Order_ and paste the following into the `java.format.imports.order` setting:
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

## Build and test the CLI

[Install Java](https://adoptium.net/installation/). This project requires Java 17.

- **Use maven:** `./mvnw install`
- **Use the Quarkus CLI**: `quarkus build`

To test with actual/live data, clone 5eTools and/or PF2eTools into a sources directory: 

```
mkdir sources
cd sources
git clone --depth 1 https://github.com/5etools-mirror-1/5etools-mirror-1.github.io.git
git clone --depth 1 https://github.com/Pf2eToolsOrg/Pf2eTools.git
```

### Build CSS only

- **build css**: `./mvnw sass-cli:run`
- **watch**: `./mvnw sass-cli:watch`
- **package**: `./mvnw sass-cli:run -Dsass.watch`

## Sign commits

I recommend that you [sign your commits](https://docs.github.com/en/authentication/managing-commit-signature-verification/signing-commits), always. For everything.

If you use SSH with Git, I have some [tips that might help](https://www.ebullient.dev/2022/10/12/signing-git-commits.html).

