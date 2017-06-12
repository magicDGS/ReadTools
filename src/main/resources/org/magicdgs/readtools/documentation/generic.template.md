<#macro argname arg>`${arg.name}`<#if arg.synonyms != "NA" && arg.synonyms != "-"><br/>`${arg.synonyms}`</#if></#macro>
<#-- TODO: use something like the following line for include range in argtype
TODO: it requires a new version of FreeMarker for accessing some mehtods
<#if arg.minValue?is_number || arg.maxValue?is_number><#if arg.minValue != "-INF" && arg.maxValue != "INF"><br/>[${arg.minValue}, ${arg.maxValue}]</#if></#if>
-->
<#macro argtype arg>${arg.type}</#macro>
<#macro argdesc arg><#if arg.fulltext != "">${arg.fulltext}<#else>${arg.summary}</#if><#if arg.options?size != 0><br/><br/><@argoptions arg=arg/></#if></#macro>
<#macro argoptions arg><b>Possible values:</b> <#list arg.options as opt><i>${opt.name}</i><#if opt.summary != ""> (${opt.summary})</#if><#if opt_has_next>, </#if></#list></#macro>
<#macro argumentlist name myargs show_default=true>
    <#if myargs?size != 0>
### ${name} Arguments

<#if name == "Deprecated" >{% include warning.html content="Do not use this arguments unless strictly necessary" %}
</#if>
| Argument name(s) | Type | <#if show_default>Default value(s) | </#if>Description |
| :--------------- | :--: | <#if show_default>:--------------: | </#if>:------ |
        <#list myargs as arg>
| <@argname arg=arg/> | <@argtype arg=arg/> | <#if show_default>${arg.defaultValue} | </#if><@argdesc arg=arg/> |
        </#list>

	</#if>
</#macro>
---
title: ${name}
summary: ${summary}
permalink: ${name}.html
last_updated: ${timestamp}
---

## Description

${description}
<#if warning?has_content>

{% include warning.html content='${warning}' %}
</#if>
<#if note?has_content>

{% include note.html content='${note}' %}
</#if>
<#if extradocs?size != 0>

<i>See additional information in the following pages:</i>

<#list extradocs as extradoc>
- [${extradoc.name}](${extradoc.name}.html)
</#list>
</#if>

<#if arguments.all?size != 0>
## Arguments

<@argumentlist name="Positional" myargs=arguments.positional show_default=false/>
<@argumentlist name="Required" myargs=arguments.required show_default=false/>
<@argumentlist name="Optional" myargs=arguments.optional/>
<@argumentlist name="Optional Common" myargs=arguments.common/>
<@argumentlist name="Advanced" myargs=arguments.advanced/>
<@argumentlist name="Deprecated" myargs=arguments.deprecated show_default=false/>

</#if>
