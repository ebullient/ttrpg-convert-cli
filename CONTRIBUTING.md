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

Two maven tasks that will format code and sort imports to reduce whitespace/ordering churn in PRs and commits.
Make sure to run `./mvnw package` or `./mvnw process-sources` before you commit, and everything will be formatted/sorted correctly.

This project also has a .editorconfig file that defines expected behavior for whitespace and line endings.

### IDE Config and Code Style

If you want your IDE to format your code for you, the files in the `ide-config` file can help.

#### Eclipse Setup

Open the *Preferences* window, and then navigate to _Java_ -> _Code Style_ -> _Formatter_. 
Click _Import_ and then select the `eclipse-format.xml` file from the `ide-config` directory.

Next navigate to _Java_ -> _Code Style_ -> _Organize Imports_. Click _Import_ and select the `eclipse.importorder` file from the `ide-config` directory.

Choose the `json5e-convert-cli` profile.

#### VSCode Setup

Add the following to your workspace settings (JSON)

```json
    "java.format.settings.url": "/full/path/to/ide-config/eclipse-format.xml",
    "java.format.settings.profile": "json5e-convert-cli",
```

## Signing

I recommend that you [sign your commits](https://docs.github.com/en/authentication/managing-commit-signature-verification/signing-commits), always. For everything.

If you use SSH with Git, I have some [tips that might help](https://www.ebullient.dev/2022/10/12/signing-git-commits.html).
 
