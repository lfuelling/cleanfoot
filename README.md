# Cleanfoot ![Java Version](https://img.shields.io/badge/java-1.8-blue.svg) ![Greenfoot version](https://img.shields.io/badge/greenfoot-3.5.4-blue.svg)

This is a fork of Greenfoot 3.5.4 (the last version supporting Java 8) which was moved from Ant to Maven. 

The goal of this project is to provide a Java 1.8 compatible, supported Greenfoot version with some fixes like a public `greenfoot.Font(java.awt.Font)` constructor, cleaned up code, up to date dependencies and of course libraries that are usable via Maven.

## Usage

If you need information regarding using Greenfoot with Maven and it's implications, please see [this blog post](https://lerks.blog/making-games-with-greenfoot-without-greenfoot/). 

If you want to use this library in your project, please replace both the `greenfoot` and the `bluej` dependency used in th eblog post above with the following:

```xml
<dependency>
    <groupId>sh.lrk</groupId>
    <artifactId>cleanfoot</artifactId>
    <version>3.5.5</version>
</dependency>
```

The library can then be used like regular Greenfoot.