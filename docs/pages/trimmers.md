---
title: Trimmer Description
summary: Algorithms used to trim the reads.
sidebar: home_sidebar
permalink: trimmers.html
toc: false
---
{% assign trimmer_groups = site.data.index.utilities | where:"group","Trimmers" %}

{% for trimmer_group in trimmer_groups %}
    {% for trimmer in trimmer_group.components %}
### [{{trimmer.name}}]({{trimmer.name}}.html)
{% if trimmer.status != "null" %} {{trimmer.status}}. {% endif %}{{trimmer.summary}}

    {% endfor %}
{% endfor %}