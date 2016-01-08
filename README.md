[![Build Status](https://travis-ci.org/dogecoin/libdohj.svg?branch=master)](https://travis-ci.org/dogecoin/libdohj)

### Welcome to libdohj

The libdohj library is a lightweight wrapper library around the bitcoinj Java library,
enabling support for Dogecoin (pull requests for support for other altcoins would
be welcomed).

### Getting started

To get started, it is best to have the latest JDK and Maven installed. The HEAD of the `master` branch contains the latest development code.
You should be familiar with bitcoinj first, as this library simply adds minor
changes to extend bitcoinj. Generally using libdohj is equivalent to using
bitcoinj, except with different network parameters (reflecting Dogecoin consensus
in place of Bitcoin).

Be aware however that altcoin blocks have their own class, AltcoinBlock, which
adds support for features such as AuxPoW.

#### Building from the command line

To perform a full build use
```
mvn clean package
```
You can also run
```
mvn site:site
```
to generate a website with useful information like JavaDocs.

The outputs are under the `target` directory.

#### Building from an IDE

Alternatively, just import the project using your IDE. [IntelliJ](http://www.jetbrains.com/idea/download/) has Maven integration built-in and has a free Community Edition. Simply use `File | Import Project` and locate the `pom.xml` in the root of the cloned project source tree.

