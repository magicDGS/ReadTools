---
title: TrailingNtrimmer
summary: Trims the end of the read containing unknown bases.
permalink: TrailingNtrimmer.html
last_updated: 25-37-2018 02:37:45
---

## Description

Trims trailing Ns (unknown bases) in the read sequence, in one or both sides.

{% include warning.html content='If a previous trimmer left the read starting/ending with Ns, the read will not
 be trimmed by the TrailingNtrimmer.' %}

