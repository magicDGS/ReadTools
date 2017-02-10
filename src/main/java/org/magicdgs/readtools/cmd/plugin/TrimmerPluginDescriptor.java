/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Daniel Gomez-Sanchez
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.magicdgs.readtools.cmd.plugin;

import org.magicdgs.readtools.cmd.RTStandardArguments;
import org.magicdgs.readtools.utils.read.transformer.trimming.TrimmingFunction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.CommandLineException;
import org.broadinstitute.barclay.argparser.CommandLinePluginDescriptor;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Plugin descriptor for including trimmers in the command line as {@link TrimmingFunction}.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 * @implNote this class have most of the code based in {@link org.broadinstitute.hellbender.cmdline.GATKPlugin.GATKReadFilterPluginDescriptor}.
 */
public final class TrimmerPluginDescriptor extends CommandLinePluginDescriptor<TrimmingFunction> {

    protected transient Logger logger = LogManager.getLogger(this.getClass());

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        logger = LogManager.getLogger(this.getClass()); // Logger is not serializable (even by Kryo)
    }

    private static final String pluginPackageName =
            "org.magicdgs.readtools.utils.read.transformer.trimming";
    private static final Class<?> pluginBaseClass = TrimmingFunction.class;

    @Argument(fullName = RTStandardArguments.TRIMMER_LONG_NAME, shortName = RTStandardArguments.TRIMMER_SHORT_NAME, doc = "Trimmers to be applied. Note: default trimmers are applied first and then the rest of them in order.", optional = true)
    public final List<String> userTrimmerNames = new ArrayList<>(); // preserve order

    // mutex because if we disable all, we cannot disable one by one
    @Argument(fullName = RTStandardArguments.DISABLE_TRIMMER_LONG_NAME, shortName = RTStandardArguments.DISABLE_TRIMMER_SHORT_NAME, doc = "Default trimmers to be disabled.", optional = true, mutex = {
            RTStandardArguments.DISABLE_ALL_DEFAULT_TRIMMERS_NAME})
    public final Set<String> disabledTrimmers = new HashSet<>();

    @Argument(fullName = RTStandardArguments.DISABLE_ALL_DEFAULT_TRIMMERS_NAME, shortName = RTStandardArguments.DISABLE_ALL_DEFAULT_TRIMMERS_NAME, doc = "Disable all default trimmers. It may be useful to reorder the trimmers.", optional = true, mutex = {
            RTStandardArguments.DISABLE_TRIMMER_LONG_NAME})
    public boolean disableAllDefaultTrimmers = false;

    @Argument(fullName = RTStandardArguments.DISABLE_5P_TRIMING_LONG_NAME, shortName = RTStandardArguments.DISABLE_5P_TRIMING_SHORT_NAME,
            doc = "Disable 5'-trimming. May be useful for downstream mark of duplicate reads, usually identified by the 5' mapping position."
                    // This is a custom mutex argument, specify as in Barclay but it could be specify in the command line
                    // TODO: see also https://github.com/broadinstitute/barclay/issues/26
                    + " Cannot be true when argument "
                    + RTStandardArguments.DISABLE_3P_TRIMING_LONG_NAME
                    + "(" + RTStandardArguments.DISABLE_3P_TRIMING_SHORT_NAME + ") is true.",
            optional = true)
    public boolean disable5pTrim = false;

    @Argument(fullName = RTStandardArguments.DISABLE_3P_TRIMING_LONG_NAME, shortName = RTStandardArguments.DISABLE_3P_TRIMING_SHORT_NAME,
            doc = "Disable 3'-trimming."
                    // This is a custom mutex argument, specify as in Barclay but it could be specify in the command line
                    // TODO: see also https://github.com/broadinstitute/barclay/issues/26
                    + " Cannot be true when argument "
                    + RTStandardArguments.DISABLE_5P_TRIMING_LONG_NAME
                    + "(" + RTStandardArguments.DISABLE_5P_TRIMING_SHORT_NAME + ") is true.",
            optional = true)
    public boolean disable3pTrim = false;

    // Map of read trimmers (simple) class names to the corresponding discovered plugin instance
    private Map<String, TrimmingFunction> trimmers = new HashMap<>();
    // all the trimmer names after discovered
    private final Set<String> allTrimmerNames = new TreeSet<>();
    // List of default trimmers in the order they were specified by the tool
    private List<String> toolDefaultTrimmerNamesInOrder = new ArrayList<>();
    // Map of read trimmers (simple) class names to the corresponding default plugin instance
    private Map<String, TrimmingFunction> toolDefaultTrimmers = new HashMap<>();
    // Set of dependent args for which we've seen values (requires predecessor)
    private Set<String> requiredPredecessors = new HashSet<>();

    /**
     * @param toolDefaultTrimmers Default trimmers that may be supplied with arguments on the
     *                            command line. May be {@code null}.
     */
    public TrimmerPluginDescriptor(final List<TrimmingFunction> toolDefaultTrimmers) {
        if (null != toolDefaultTrimmers) {
            toolDefaultTrimmers.forEach(f -> {
                // validate the argument from the defaults
                try {
                    // should validate unsafely because it should set afterwards the validation
                    f.validateArgsUnsafe();
                } catch (CommandLineException | UserException e) {
                    throw new IllegalArgumentException("Not valid arguments in default trimmer: "
                            + f, e);
                }
                final Class<? extends TrimmingFunction> rfClass = f.getClass();
                // anonymous classes have a 0-length simple name, and thus cannot be accessed or
                // controlled by the user via the command line, but they should still be valid
                // as default trimmers, so use the full name to ensure that their map entries
                // don't clobber each other
                String className = rfClass.getSimpleName();
                if (className.isEmpty()) {
                    className = rfClass.getName();
                }
                toolDefaultTrimmerNamesInOrder.add(className);
                this.toolDefaultTrimmers.put(className, f);
            });
        }
    }

    @Override
    public String getDisplayName() {
        return RTStandardArguments.TRIMMER_LONG_NAME;
    }

    @Override
    public Class<?> getPluginClass() {
        return pluginBaseClass;
    }

    // we require to remove the base class, which is abstract
    @Override
    public Predicate<Class<?>> getClassFilter() {
        return c -> {
            // don't use the base class nor the unit test implementations
            return !c.getName().equals(this.getPluginClass().getName())
                    && !c.getName().contains("UnitTest$");
        };
    }

    @Override
    public List<String> getPackageNames() {
        return Collections.singletonList(pluginPackageName);
    }

    @Override
    public Object getInstance(Class<?> pluggableClass)
            throws IllegalAccessException, InstantiationException {
        TrimmingFunction trimmingFunction;
        final String simpleName = pluggableClass.getSimpleName();

        if (trimmers.containsKey(simpleName)) {
            // we found a plugin class with a name that collides with an existing class;
            // plugin names must be unique even across packages
            throw new IllegalArgumentException(
                    String.format("A plugin class name collision was detected (%s/%s). " +
                                    "Simple names of plugin classes must be unique across packages.",
                            pluggableClass.getName(),
                            trimmers.get(simpleName).getClass().getName())
            );
        } else if (toolDefaultTrimmers.containsKey(simpleName)) {
            // an instance of this class was provided by the tool as one of it's default trimmers;
            // use the default instance as the target for command line argument values
            // rather than creating a new one in case it has state provided by the tool
            trimmingFunction = toolDefaultTrimmers.get(simpleName);
        } else {
            trimmingFunction = (TrimmingFunction) pluggableClass.newInstance();
            trimmers.put(simpleName, trimmingFunction);
        }
        // add to the trimmer names
        allTrimmerNames.add(simpleName);
        return trimmingFunction;
    }

    @Override
    public Set<String> getAllowedValuesForDescriptorArgument(String longArgName) {
        if (longArgName.equals(RTStandardArguments.TRIMMER_LONG_NAME)) {
            // in the case of the trimmer argument, return all the names obtained by reflection
            return allTrimmerNames;
        }
        if (longArgName.equals(RTStandardArguments.DISABLE_TRIMMER_LONG_NAME)) {
            // linked set to return then in order
            return new LinkedHashSet<>(toolDefaultTrimmerNamesInOrder);
        }
        throw new IllegalArgumentException(
                "Allowed values request for unrecognized string argument: " + longArgName);
    }

    @Override
    public boolean isDependentArgumentAllowed(Class<?> dependentClass) {
        // make sure the predecessor for this dependent class was either specified
        // on the command line or is a tool default, otherwise reject it
        String predecessorName = dependentClass.getSimpleName();
        boolean isAllowed = userTrimmerNames.contains(predecessorName)
                || (toolDefaultTrimmers.get(predecessorName) != null);
        if (isAllowed) {
            // keep track of the ones we allow so we can validate later that they
            // weren't subsequently disabled
            requiredPredecessors.add(predecessorName);
        }
        return isAllowed;
    }

    @Override
    public void validateArguments() throws CommandLineException {
        // validate here the arguments for disabling 5/3 prime
        if (disable3pTrim && disable5pTrim) {
            // TODO: contribute to Barclay to create special CommandLineException for this cases
            // TODO: to be able to apply them in other parts of the code
            throw new CommandLineException("Argument '"
                    + RTStandardArguments.DISABLE_5P_TRIMING_LONG_NAME
                    + "' cannot be used in conjunction with argument(s)"
                    + RTStandardArguments.DISABLE_5P_TRIMING_LONG_NAME);
        }


        // throw if any trimmer is specified twice
        final List<String> moreThanOnce = userTrimmerNames.stream()
                .collect(Collectors.groupingBy(e -> e, Collectors.counting()))
                .entrySet().stream().filter(e -> e.getValue() != 1)
                .map(e -> e.getKey() + " (" + e.getValue() + ")")
                .collect(Collectors.toList());
        if (!moreThanOnce.isEmpty()) {
            throw new CommandLineException.BadArgumentValue(
                    String.format("Trimmer(s) are specified more than once: %s",
                            Utils.join(", ", moreThanOnce)));
        }

        // throw if any trimmer is both enabled *and* disabled by the user
        final Set<String> enabledAndDisabled = new HashSet<>(userTrimmerNames);
        enabledAndDisabled.retainAll(disabledTrimmers);
        if (!enabledAndDisabled.isEmpty()) {
            throw new CommandLineException.BadArgumentValue(
                    String.format("Trimmer(s) are both enabled and disabled: %s",
                            Utils.join(", ", enabledAndDisabled)));
        }

        // warn if a disabled trimmer wasn't enabled by the tool in the first place
        disabledTrimmers.forEach(s -> {
            if (!toolDefaultTrimmers.containsKey(s)) {
                logger.warn("Disabled trimmer ({}) is not enabled by this tool", s);
            }
        });

        // warn on redundant enabling of trimmer already enabled by default
        if (!disableAllDefaultTrimmers) {
            final Set<String> redundant = new HashSet<>(toolDefaultTrimmers.keySet());
            redundant.retainAll(userTrimmerNames);
            redundant.forEach(s -> logger
                    .warn("Redundant enabled trimmer ({}) is enabled for this tool by default", s)
            );
        }
        // throw if args were specified for a trimmer that was also disabled
        disabledTrimmers.forEach(s -> {
            if (requiredPredecessors.contains(s)) {
                if (toolDefaultTrimmers.containsKey(s)) {
                    // TODO: this should blow up if the argument was really provided
                    logger.warn("Values were supplied for disabled default trimmer ({})", s);
                } else {
                    throw new CommandLineException.BadArgumentValue(
                            String.format("Values were supplied for (%s) that is also disabled",
                                    s));
                }
            }
        });

        // throw if a trimmer name was specified that has no corresponding instance
        final Map<String, TrimmingFunction> requestedTrimmers = new HashMap<>();
        userTrimmerNames.forEach(s -> {
            TrimmingFunction trf = trimmers.get(s);
            if (null == trf) {
                if (!toolDefaultTrimmers.containsKey(s)) {
                    throw new CommandLineException.BadArgumentValue(
                            String.format("Unrecognized trimmer name: %s", s));
                }
            } else {
                requestedTrimmers.put(s, trf);
            }
        });
        // validates tool default trimmers arguments (maybe overridden in command line)
        // and set the disable5pTrim and disable3primeTrim for the trimmers in use (tool/requested)
        validateAndSetDisablingEnds(toolDefaultTrimmers);
        validateAndSetDisablingEnds(requestedTrimmers);

        // update the trimmers list with the final list of trimmer specified on the
        // command line; do not include tool defaults as these will be merged in at merge
        // time if they were not disabled
        trimmers = requestedTrimmers;
    }

    // validates the arguments and also set the disable ends for all trimmers in the list
    private void validateAndSetDisablingEnds(final Map<String, TrimmingFunction> nameTrimmerMap) {
        nameTrimmerMap.forEach((name, trimmer) -> {
            // first set, because they may be used in the validation
            logger.debug("{} trimmer: 5 prime {} and 3 prime {}", () -> name,
                    () -> (disable5pTrim) ? "disabled" : "not disabled",
                    () -> (disable3pTrim) ? "disabled" : "not disabled");
            trimmer.setDisableEnds(disable5pTrim, disable3pTrim);
            trimmer.validateArgs();
        });
    }

    /**
     * Gets all the default instances provided; if disabling all the default instances is
     * specified, returns an empty list.
     *
     * Note: only the enabled trimmers will be returned.
     */
    //
    // TODO: the signature should be change in Barclay: https://github.com/broadinstitute/barclay/pull/32
    @Override
    public List<Object> getDefaultInstances() {
        return (disableAllDefaultTrimmers)
                ? Collections.emptyList()
                : toolDefaultTrimmerNamesInOrder.stream()
                        .filter(s -> !disabledTrimmers.contains(s))
                        .map(s -> toolDefaultTrimmers.get(s))
                        .collect(Collectors.toList());
    }

    /**
     * Gets all the instances provided by the user. It may return an empty list if the user does
     * not specify any tool or only the default ones (without disable then completely).
     *
     * Note: if all default trimmers are disable, this list include all the command line ones;
     * otherwise, the defaults will be excluded.
     */
    @Override
    public List<TrimmingFunction> getAllInstances() {
        // start with the tool's default trimmers in the order they were specified, and remove any
        // that were disabled on the command line

        // Add the instances in the order they were specified on the command line
        // (use the order of userTrimmerNames list, so it preserves the order).
        //
        // NOTE: it's possible for the userTrimmerNames list to contain one or more
        // names for which there are no corresponding instances in the trimmers list.
        // This happens when the user specifies a trimmer name on the command line that's
        // already included in the toolDefault list, since in that case the descriptor
        // uses the tool-supplied instance and doesn't add a separate one to the
        // trimmers list, but the name from the command line still appears in
        // userTrimmerNames.
        return userTrimmerNames.stream()
                .map(s -> {
                    TrimmingFunction tf = trimmers.get(s);
                    if (tf != null) {
                        return tf;
                    } else if (disableAllDefaultTrimmers) {
                        return toolDefaultTrimmers.get(s);
                    } else {
                        return null;
                    }
                })
                .filter(tf -> tf != null)
                .collect(Collectors.toList());
    }

    @Override
    public Class<?> getClassForInstance(String pluginName) {
        return trimmers.get(pluginName).getClass();
    }
}
