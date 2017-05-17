---
title: ReadTools Java Properties
sidebar: home_sidebar
permalink: readtools_java_properties.html
toc: false
---

{% include warning.html content="We discourage the use of this properties unless it is necessary." %}

Some advanced parameters could be tweaked using Java properties. This properties could be set in the command line by running the jar file as following:

```bash
java -Dreadtools.${property_name}=${property_value} -jar ReadTools.jar ${your_arguments}
```


| Property name             | Default  | Description |
| :------------------------------------ | :----------: | :---------- |
| `barcode_index_delimiter`             | `-`          | Delimiter between barcode sequence when several indexes are used |
| `barcode_quality_delimiter`           | ` `          | Delimiter between barcode quality when several indexes are used |
| `max_record_for_quality`              | `1000000`    | Maximum number of record used to guess the quality of a file |
| `sampling_quality_checking_frequency` | `1000`       | Read sampling frequency to check if the quality is really Standard |
| `force_overwrite`                     | `false`      | Force overwrite of output files (default value) |
| `discarded_output_suffix`             | `_discarded` | Suffix for discarded output file(s) |
