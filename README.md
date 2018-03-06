[![Build Status](https://travis-ci.org/dogecoin/libdohj.svg?branch=master)](https://travis-ci.org/dogecoin/libdohj)

### Welcome to libdohj

The libdohj library is a lightweight wrapper library around the bitcoinj Java library,
enabling support for Dogecoin (pull requests for support for other altcoins would
be welcomed).

### Technologies

* Java 7 for the core modules, Java 8 for everything else
* [Gradle 3.4+](https://gradle.org/) - for building the project
* [Google Protocol Buffers](https://github.com/google/protobuf) - for use with serialization and hardware communications

### Getting started

To get started, it is best to have the latest JDK and Gradle installed. The HEAD of the `main` branch contains the latest development code and various production releases are provided on feature branches.

You should be familiar with bitcoinj first, as this library simply adds minor changes to extend bitcoinj. Generally using libdohj is equivalent to using bitcoinj, except with different network parameters (reflecting Dogecoin consensus in place of Bitcoin).

Be aware however that altcoin blocks have their own class, AltcoinBlock, which adds support for features such as AuxPoW.

#### Building from the command line

To perform a full build use
```
gradle clean build
```
You can also run
```
gradle javadoc
```
to generate the JavaDocs.

The outputs are under the `build` directory.

#### Building from an IDE

Alternatively, just import the project using your IDE. [IntelliJ](http://www.jetbrains.com/idea/download/) has Gradle integration built-in and has a free Community Edition. Simply use `File | New | Project from Existing Sources` and locate the `build.gradle` in the root of the cloned project source tree.

