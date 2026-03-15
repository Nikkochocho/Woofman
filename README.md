# Woofman

<div align = "center">

<img src = "/doc/resources/Ruffus.png" alt = "woofman logo" title = "Ruffus">

`Woofman` is an open source encoding/decoding tool written in Java that uses Huffman algorithm to perform lossless data compression and decompression.

This project includes files saved in different formats as samples.

</div>

## Table of Contents :pushpin:
* [Features](#features-hatching_chick)
* [Requirements](#requirements-memo)
    - [Java 11](#java-11)
* [HowTo](#howto-rocket)
    - [Using Samples](#using-samples-dog)

## Features :hatching_chick:

Here are the main features `Woofman` contains:
- An **Encoder** for data compression
- A **Decoder** for data decompression
- A folder of **Samples** to try and test its features

## Requirements :memo:

### Java 11

To run `Woofman` as intended, you should acquire Java 11 or higher. Please ensure it is installed before running the application as shown.

```shell
# Verify installation
java -version
```
If you do not have Java 11 or any higher version installed, you can install the OpenJDK package on its official page by clicking [here](https://openjdk.org/).

- **Ubuntu/Debian family**

    Ubuntu/Debian users can install the package by using the following command on their Linux shell:

    ```shell
    sudo apt install openjdk-11-jdk
    ```

- **MacOS**

    MacOS user can install OpenJDK through [Homebrew](https://brew.sh/).

## HowTo :rocket:

Before running the project, be sure to compile it using the following command:

```shell
javac -d out src/*.java
```

- For data **compression**

```shell
java Main encode <original_file> <encoded_file>
```

- For data **decompression**

```shell
java Main decode <encoded_file> <decoded_file>
```

> [!TIP]
> If you enter the commands the wrong way, a help message will appear to show the correct usage.

### Using Samples :dog:

`Woofman` samples are located in the **samples/** folder. To use them, just enter the relative path to their location as shown before.