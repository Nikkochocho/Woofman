# Woofman

<div align = "center">

<img src = "/doc/resources/Ruffus.png" alt = "woofman logo" title = "Ruffus">

`Woofman` is an open source encoding/decoding tool written in Java that implements a whole family of lossless data compression algorithms — from classic entropy coders like Huffman and arithmetic coding, to dictionary-based methods like LZ77/LZW, to block-sorting transforms like BWT — and lets you pick the best one per file.

This project includes files saved in different formats as samples.

</div>

## Table of Contents :pushpin:
* [Features](#features-hatching_chick)
* [Algorithms](#algorithms-package)
    - [Entropy Coding](#entropy-coding)
    - [Dictionary Coding](#dictionary-coding)
    - [Block-Sorting](#block-sorting)
    - [Filters](#filters)
* [Requirements](#requirements-memo)
    - [Java 21](#java-21)
* [HowTo](#howto-rocket)
    - [Choosing an Algorithm](#choosing-an-algorithm-thinking)
    - [Using Samples](#using-samples-dog)

## Features :hatching_chick:

Here are the main features `Woofman` contains:
- An **Encoder** for data compression, packing one or more files/directories into a single `.woof` container
- A **Decoder** for data decompression, restoring the original file(s) from a `.woof` container
- **10+ compression algorithms** to choose from, spanning entropy coding, dictionary coding, and block-sorting transforms
- **Smart algorithm suggestions** per file, based on file type detection (text, image, audio, generic binary, etc.)
- A folder of **Samples** to try and test its features

## Algorithms :package:

`Woofman` is built around a small set of independent building blocks that get combined to form the final algorithms below. Every algorithm implements the same `CompressionAlgorithm` interface, so any of them can compress or decompress any file — the suggestions are just a starting point, not a restriction.

### Entropy Coding

| Algorithm | Description |
|---|---|
| **Huffman** | Classic prefix-code entropy coder |
| **Range** | Adaptive/static range coder, with a Krichevsky–Trofimov selector deciding automatically which model fits the data better |
| **Arithmetic** | Bit-level arithmetic coder (32-bit precision), same static/dynamic selector as Range |
| **RLE** | Run-length encoding, best for data with long runs of repeated bytes |

### Dictionary Coding

LZ77 and LZW handle the repetition-finding part, and are then paired with one of the entropy coders above to squeeze the residual statistical redundancy out of their output:

| Algorithm | Combination |
|---|---|
| **DEFLATE** | LZ77 tokenization + Huffman coding |
| **LZMA** | LZ77 + Range coding |
| **LZARI** | LZ77 + Arithmetic coding |
| **LZW + Huffman** | LZW tokenization + Huffman coding |
| **LZW + Range** | LZW + Range coding |
| **LZW + Arithmetic** | LZW + Arithmetic coding |

### Block-Sorting

The BWT family reorders bytes by context (instead of chasing literal repetitions), which tends to outperform LZ-based methods on plain text, uncompressed images/audio, and other data with strong statistical — rather than literal — redundancy:

| Algorithm | Combination |
|---|---|
| **BWT** | Burrows–Wheeler Transform + Move-To-Front + adaptive Range coding |
| **bzip2-style** | BWT + Move-To-Front + RLE0 (zero-run encoding) + adaptive Range coding |

### Filters

Some formats benefit from a format-aware preprocessing step *before* entropy coding — reshaping the bytes so their redundancy becomes easier for Huffman/Range/Arithmetic to exploit, based on how that format actually stores data:

| Format | Filter | Description |
|---|---|---|
| **BMP** | Paeth | Predicts each pixel from its left, top, and top-left neighbors (same predictor PNG uses) and stores the residual — turns smooth color gradients into near-zero values |
| **WAV** | Delta encoding | Stores the difference between consecutive audio frames instead of raw sample values — exploits how amplitude changes gradually between samples |

Both filters are paired with Huffman coding to form a dedicated algorithm for their format (`BMP + Paeth + Huffman`, `WAV + Delta + Huffman`), automatically suggested when `Woofman` detects a matching file type. BMP also gets transparent RLE8 decoding when the source file already uses that compression mode, so the Paeth filter always operates on raw pixel data.

## Requirements :memo:

### Java 21

`Woofman` relies on modern Java language features (sealed interfaces, records, and pattern matching for `switch`), so you'll need **Java 21 or higher** to compile and run it. Please ensure it is installed before running the application as shown.

```shell
# Verify installation
java -version
```
If you do not have Java 21 or any higher version installed, you can install the OpenJDK package on its official page by clicking [here](https://openjdk.org/).

- **Ubuntu/Debian family**

    Ubuntu/Debian users can install the package by using the following command on their Linux shell:

    ```shell
    sudo apt install openjdk-21-jdk
    ```

- **MacOS**

    MacOS user can install OpenJDK through [Homebrew](https://brew.sh/).

## HowTo :rocket:

Before running the project, be sure to compile it using the following command:

```shell
javac -d out $(find . -name "*.java")
```

- For data **compression**

```shell
java -cp out cli.Main encode <source>
```

`<source>` can be a single file or a whole directory — in the latter case, every file inside it is collected and compressed together into one `.woof` container.

- For data **decompression**

```shell
java -cp out cli.Main decode <compressed_file>
```

> [!TIP]
> If you enter the commands the wrong way, a help message will appear to show the correct usage.

### Choosing an Algorithm :thinking:

During `encode`, `Woofman` inspects each file and suggests a shortlist of algorithms that tend to work well for that kind of content, then asks you to pick one:

```shell
File: example.txt - Encode options [1] Huffman [2] Range [3] BWT [4] bzip2 ...
> 4
```

Once every file has an algorithm assigned, compression runs and reports the rate achieved for each one.
Since every algorithm can compress any file, feel free to go against the suggestions and experiment — that's the best way to see how entropy coding, dictionary coding, and block-sorting each respond to different kinds of data.

### Using Samples :dog:

`Woofman` samples are located in the **samples/** folder. To use them, just enter the relative path to their location as shown before.