---
title: Frequently Asked Questions
sidebar: home_sidebar
permalink: faq.html
toc: false
---

{% include faq/start.html %}

{% include faq/entry.html
  question='Exception in thread "main" java.lang.NoClassDefFoundError: org/xerial/snappy/LoadSnappy'
  content='If your command fail with this error, the solution is to re-run it using
  <code>java -Dsnappy.disable=true -jar ReadTools.jar</code>.
  <p>This is a known bug of one of the libraries used in ReadTools that may be solved in the future.</p>'
%}

{% include faq/end.html %}
