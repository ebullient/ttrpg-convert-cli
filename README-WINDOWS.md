# Running on Windows

We're going to assume a user named `Xanathar` in this guide. 
- When you use the example commands, replace `Xanathar` with your user name.

## How the Windows command prompt works

You can Open the Windows command prompt by pressing the Windows key and entering cmd.

- To the left of the cursor you should see something like `C:\Users\Xanathar`. 
  The Command prompt is currently active in that folder. 

- To navigate elsewhere, use the `cd` command.  
  For example, to get to your `Downloads` folder from here, type `cd Downloads` and hit `Enter`. 
  The prompt on the left should now say `C:\Users\Xanathar\Downloads`. 

- To see the folders you can reach this way, type `dir` and hit `Enter`. 
  You'll be presented with a table, the rightmost column listing all of the folder contained by the active folder.

## Downloading materials

This is one example with specific steps. If you know a little more about running commands on Windows, feel free to go your own way.

1.  Download both the [latest release][1] and the [code as a zip file][2] (because you'll want to use the contents of the examples folder later).

2. Unzip the downloaded files and make sure the folder structure doesn't contain duplicated/nested folders

    You want `ttrpg-convert-cli-2.0.4` to contain bin, LICENSE, and README.md, not another folder called ttrpg-convert-cli-2.0.4; this goes especially for the 5etools data.

3. Put the `ttrpg-convert-cli-2.0.4` folder in any place you like. 
    
    For this example, you'll be working from within the bin folder, which contains the file ttrpg-convert.exe. 
    
    1. Move the other two folders you extracted (`5etools-mirror-1.github.io-master` and `ttrpg-convert-cli-main`) inside of the bin directory.

    2. If you leave the extracted `ttrpg-convert-cli-2.0.4` folder in the Downloads directory, the following commands should work: 

        1. Make the `Downloads` directory active. When you list the folders, it should include `ttrpg-convert-cli-2.0.4`.
            ```
            cd C:\Users\Xanathar\Downloads
            dir
            ```

        2. Make the `ttrpg-convert-cli` active. When you list the folders, it should include `bin`.
            ```
            cd ttrpg-convert-cli-2.0.4
            dir
            ```
    
        3. Make the `bin` active. When you list the folders, it should include `5etools-mirror-1.github.io-master` and `ttrpg-convert-cli-main`.
            ```
            cd bin
            dir
            ```    

[1]: https://github.com/ebullient/ttrpg-convert-cli/releases/latest
[2]: https://github.com/ebullient/ttrpg-convert-cli/archive/refs/heads/main.zip

## Running the CLI

1. Open Command Prompt. 

2. Navigate to the bin folder.

    - If you followed the example above, you can do: `cd C:\Users\Xanathar\Downloads\ttrpg-convert-cli-2.0.4\bin`
    - Alternately, navigate to the ttrpg-convert-cli-2.0.4 folder in File Explorer and copy the path from the address bar. 
        Then go back to Command Prompt, type `cd ` and paste the path you just copied, and hit `Enter`. 
        The prompt on the left should now look like this: `<path to the folder you moved ttrpg-convert-cli into>\ttrpg-convert-cli-2.0.4\bin`

3. Type `ttrpg-convert.exe --index -o dm 5etools-mirror-1.github.io-master/data` and hit `Enter`.

    A new folder called `dm` should have been created containing the entire D&D5e SRD. If that's all you need, you're done.

4. If you want to add additional sources, templates, or books, create a `dm-sources.json` file. 
    - The contents of the config file is [described in the README][3].
    - Use this command to use your custom configuration:
        ```
        ttrpg-convert.exe --index -o dm -c dm-sources.json 5etools-mirror-1.github.io-master/data
        ```

[3]: https://github.com/ebullient/ttrpg-convert-cli/blob/main/README.md#additional-parameters
