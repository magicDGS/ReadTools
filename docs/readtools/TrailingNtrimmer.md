---
title: TrailingNtrimmer
summary: Trims the end of the read containing unknown bases.
permalink: TrailingNtrimmer.html
last_updated: 11-25-2018 03:25:45
---


## Description

Trims trailing Ns (unknown bases) in the read sequence, in one or both sides.

{% include warning.html content='If a previous trimmer left the read starting/ending with Ns, the read will not
 be trimmed by the TrailingNtrimmer.' %}

