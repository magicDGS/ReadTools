---
title: Read Filters Description
sidebar: home_sidebar
permalink: filters.html
toc: false
---
{% assign filter_groups = site.data.index.utilities | where:"group","Read Filters" %}

{% for filter_group in filter_groups %}
    {% for filter in filter_group.components %}
### [{{filter.name}}]({{filter.name}}.html)
{{filter.summary}}

    {% endfor %}
{% endfor %}