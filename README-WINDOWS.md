# Running on Windows

> [!TIP]
> See also Obsidian TTRPG Tutorials: [TTRPG-Convert-CLI 5e][] or [TTRPG-Convert-CLI PF2e][]

[TTRPG-Convert-CLI 5e]: https://obsidianttrpgtutorials.com/Obsidian+TTRPG+Tutorials/Plugin+Tutorials/TTRPG-Convert-CLI/TTRPG-Convert-CLI+5e
[TTRPG-Convert-CLI PF2e]: https://obsidianttrpgtutorials.com/Obsidian+TTRPG+Tutorials/Plugin+Tutorials/TTRPG-Convert-CLI/TTRPG-Convert-CLI+PF2e

We're going to assume a user named `Xanathar` in this guide.

- When you use the example commands, replace `Xanathar` with your user name.

## How the Windows command prompt works

You can Open the Windows command prompt by pressing the Windows key and entering `cmd`.

- The text to the left of the cursor (e.g. `C:\Users\Xanathar`) is your current directory.

- To navigate elsewhere, use the `cd` command to "change directory".  
  For example, to get to your `Downloads` folder from here, type `cd Downloads` and hit `Enter`.
  The prompt on the left should now say `C:\Users\Xanathar\Downloads`.

- To see the folders you can reach this way, type `dir` and hit `Enter`.
  You'll be presented with a table, the rightmost column listing all of the folder contained by the active folder.

## Downloading materials

This is one example with specific steps. If you know more about running commands on Windows, feel free to go your own way.

1. From the [latest release][1], download the following files:

    - `ttrpg-convert-cli-2.3.5-windows-x86_64.zip`
    - `ttrpg-convert-cli-2.3.5-examples.zip`

2. Unzip the downloaded files and make sure the folder structure doesn't contain duplicated/nested folders

    You want `ttrpg-convert-cli-2.3.5` to contain bin, LICENSE, and README.md, not another folder called `ttrpg-convert-cli-2.3.5`.

3. Put the `ttrpg-convert-cli-2.3.5` folder in any place you like.

    For this example, you'll be working from within the bin folder, which contains the file `ttrpg-convert.exe`.

    1. Move the other two folders you extracted (`5etools-mirror-2.github.io-master` and `ttrpg-convert-cli-2.3.5-examples`) inside of the bin directory.

    2. If you leave the extracted `ttrpg-convert-cli-2.3.5` folder in the Downloads directory, the following commands should work:

        1. Make the `Downloads` directory active. When you list the folders, it should include `ttrpg-convert-cli-2.3.5`.

            ```console
            cd C:\Users\Xanathar\Downloads
            dir
            ```

        2. Make the `ttrpg-convert-cli` active. When you list the folders, it should include `bin`.

            ```console
            cd ttrpg-convert-cli-2.3.5
            dir
            ```

        3. Make the `bin` active. When you list the folders, it should include `5etools-mirror-2.github.io-master` and `ttrpg-convert-cli-2.3.5-examples`.

            ```console
            cd bin
            dir
            ```

[1]: https://github.com/ebullient/ttrpg-convert-cli/releases/latest

## Running the CLI

1. Open Command Prompt.

2. Navigate to the bin folder.

    - If you followed the example above, you can do: `cd C:\Users\Xanathar\Downloads\ttrpg-convert-cli-2.3.5\bin`
    - Alternately, navigate to the `ttrpg-convert-cli-2.3.5` folder in File Explorer and copy the path from the address bar.
        Then go back to Command Prompt, type `cd` and paste the path you just copied, and hit `Enter`.
        The prompt on the left should now look like this: `<path to the folder you moved ttrpg-convert-cli into>\ttrpg-convert-cli-2.3.5\bin`

3. Type `ttrpg-convert.exe --index -o dm 5etools-mirror-2.github.io-master/data` and hit `Enter`.

    A new folder called `dm` should have been created containing the entire D&D5e SRD. If that's all you need, you're done.

4. If you want to add additional sources, templates, or books, [create a config file][3].

    - Use this command to use your custom configuration (assuming a file called `dm-sources.json`):

        ```console
        ttrpg-convert.exe --index -o dm -c dm-sources.json 5etools-mirror-2.github.io-master/data
        ```

[3]: docs/configuration.md

## Uh oh, something went wrong

If you see the following:

> "The current machine does not support all of the following CPU features that are required by the image:
> \[CX8, CMOV, FXSR, MMX, SSE, SSE2, SSE3, SSSE3, SSE4_1, SSE4_2, POPCNT, LZCNT, AVX, AVX2, BMI1, BMI2, FMA].
> Please rebuild the executable with an appropriate setting of the -march option."

You have an older version of Windows. You'll need to use the [Java version](docs/alternateRun.md#use-java-to-run-the-jar) of the CLI instead.
