---
title: Trimming Pipelines
sidebar: home_sidebar
permalink: trimming_pipelines.html
---
{% assign trimmer_groups = site.data.index.utilities | where:"group","Trimmers" %}
{% assign filter_groups = site.data.index.utilities | where:"group","Read Filters" %}

[TrimReads](TrimReads.html) applies a trimming/filtering pipeline that can be highly customized by the user. The tool includes default trimmers/filters, but they could be disabled or other ones included. The order of the pipeline is the following:

1. Default trimmers
2. User-specified trimmers
3. Default filters
4. User-specified filters

This order is important, because some trimmers would not apply in some situations. For example, if the read is already trimmed in a right-most position for 5' when it is passed to another trimmer, the 5' is not trimmed any further. For reordering defaults, specify `--disableAllDefaultTrimmers` and provide them in the new order.

## Trimmers

The following trimmers could be applied in the pipeline. Click on the trimmer name to see more information.

  {% for trimmer_group in trimmer_groups %}
      {% for trimmer in trimmer_group.components %}
  - [{{trimmer.name}}]({{trimmer.name}}.html): {{trimmer.summary}}
      {% endfor %}
  {% endfor %}

## Filters

The following filters could be applied in the pipeline. Click on the filter name to see more information.

{% include note.html content='Some filters may be undocumented because they are implemented in GATK4, which is unreleased' %}

  {% for filter_group in filter_groups %}
      {% for filter in filter_group.components %}
  - [{{filter.name}}]({{filter.name}}.html): {% if filter.summary == nil %}_No summary available._{% else %}{{filter.summary}}{% endif %}
      {% endfor %}
  {% endfor %}
