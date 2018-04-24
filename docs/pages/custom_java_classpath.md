---
title: Custom classpath 
sidebar: home_sidebar
permalink: custom_java_classpath.html
toc: true
---

## When to use a custom classpath?

_ReadTools_ relies on several libraries using Service Provider Interfaces
(SPI) for extensible applications. Some common use cases to add an
extension in _ReadTools_ are:

- `java.nio.file.spi.FileSystemProvider` for IO operations in different
  file systems.
- `org.apache.hadoop.io.compress.CompressionCodec` for custom compression
  for IO in Hadoop.
- Other Hadoop services.

## How to run _ReadTools_ with a custom classpath

For an unique jar file containing the extension for any service, named
_**service.jar**_:

```bash
java -cp ReadTools.jar:service.jar org.magicdgs.readtools.Main
```

If several services are in different jar files, the `-cp` option might
have a list of the jars separated by `:`.

## Bundled services

_ReadTools_ jar file already packages several SPI extensions in its main
jar, providing out-of-the-box support for:

- [Hadoop File System](https://hadoop.apache.org/docs/r1.2.1/hdfs_user_guide.html) (HDFS) paths
- [Google Cloud Storage](https://cloud.google.com/storage/) (GCS) paths
- Hadoop defaults

## Example usage

One common usage of the custom classpath is to support in your Hadoop
cluster non-default compression format, which integrates with the
[Distmap pipeline](distmap.html).

For example, [4mc](https://github.com/carlomedas/4mc) compression would
make upload/download faster. You can download the packaged jar (e.g.,
[`hadoop-4mc-2.0.0.jar`](https://github.com/carlomedas/4mc/releases/download/2.0.0/hadoop-4mc-2.0.0.jar)).
File names ending in `.4mc` would be output as compressed files with this
compressor if run as following:

```bash
java -cp ReadTools.jar:hadoop-4mc-2.0.0.jar org.magicdgs.readtools.Main \
         ReadsToDistmap -I input.bam -O hdfs://output.4mc
```
