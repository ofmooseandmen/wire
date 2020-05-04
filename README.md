# Wire

[![CI](https://github.com/ofmooseandmen/wire/workflows/CI/badge.svg)](https://github.com/ofmooseandmen/wire/actions?query=workflow%3ACI)
[![codecov.io](https://codecov.io/github/ofmooseandmen/wire/branches/master/graphs/badge.svg)](https://codecov.io/github/ofmooseandmen/wire)
[![license](https://img.shields.io/badge/license-BSD3-lightgray.svg)](https://opensource.org/licenses/BSD-3-Clause)

An implementation of the Google Cast V2 protocol in Java.

## Acknowledgement

Hat tip to [github.com/thibauts](https://github.com/thibauts) for his detailed description of the [Cast V2 protocol](https://github.com/thibauts/node-castv2#protocol-description)!

## Building from Source

You need [JDK-8](http://openjdk.java.net/projects/jdk8/) or higher to build Wire.
Wire can be built with Gradle using the following command:

```
./gradlew clean build
```

## Tests

Wire is tested with [cucumber](https://cucumber.io). Feature files can be found in the `src/test/resources` folder.

