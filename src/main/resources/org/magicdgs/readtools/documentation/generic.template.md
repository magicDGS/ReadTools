<#macro argumentlist name myargs>
    <#if myargs?size != 0>
### ${name}

| Argument name(s) | Type | Default value(s) | Summary |
| :--------------- | :--: | :--------------: | :------ |
        <#list myargs as arg>
| `${arg.name}`<#if arg.synonyms != "NA">,`${arg.synonyms}`</#if> | ${arg.type} | ${arg.defaultValue} | ${arg.summary} |
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

<#if arguments.all?size != 0>
## Arguments

<@argumentlist name="Positional Arguments" myargs=arguments.positional/>
<@argumentlist name="Required Arguments" myargs=arguments.required/>
<@argumentlist name="Optional Arguments" myargs=arguments.optional/>
<@argumentlist name="Optional Common Arguments" myargs=arguments.common/>
<@argumentlist name="Advanced Arguments" myargs=arguments.advanced/>
<@argumentlist name="Deprecated Arguments" myargs=arguments.deprecated/>

</#if>
