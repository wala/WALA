/*
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This file is a derivative of code released under the terms listed below.  
 *
 */
/*
 *  Copyright (c) 2013,
 *      Tobias Blaschke <code@tobiasblaschke.de>
 *  All rights reserved.

 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 *  3. The names of the contributors may not be used to endorse or promote
 *     products derived from this software without specific prior written
 *     permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */
package com.ibm.wala.dalvik.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.Intent;

/**
 *  Read in an extracted AndroidManifest.xml.
 *
 *  The file has to be in the extracted (human readable) XML-Format. You can extract it using the program
 *  `apktool`.
 *
 *  Tags and Attributes not known by the Parser are skipped over.
 *
 *  To add a Tag to the parsed ones:
 *      You have to extend the enum Tags: All Tags on the path to the Tag in question have to be in the enum.
 *      Eventually you will have to adapt the ParserItem of the Parent-Tags and add it to their allowed Sub-Tags. 
 *
 * To add an Attribute to the handled ones:
 *      You'll have to extend the Enum Attrs if the Attribute-Name is not yet present there. Then add the 
 *      Attribute to the relevant Attributes of it's containing Tag.
 *
 *      You will be able to access it using attributesHistory.get(Attr).peek()
 *
 * TODO:
 * TODO: Handle Info in the DATA-Tag correctly!
 * @since   2013-10-13
 * @author  Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 */
public class AndroidManifestXMLReader {
    /**
     *  This logs low-level parsing.
     *
     *  Mainly pushes and pops from stacks and seen tags.
     *  If you only want Information about objects created use the logger in AndroidSettingFactory as this 
     *  parser generates all objects using it.
     */
    private static final Logger logger = LoggerFactory.getLogger(AndroidSettingFactory.class);

    public AndroidManifestXMLReader(File xmlFile) {
        if (xmlFile == null) {
            throw new IllegalArgumentException("xmlFile may not be null");
        }
        try (final FileInputStream in = new FileInputStream(xmlFile)) {
            readXML(in);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException("Exception was thrown");
        }
    }

    public AndroidManifestXMLReader(InputStream xmlFile) {
        if (xmlFile == null) {
            throw new IllegalArgumentException("xmlFile may not be null");
        }
        try {
            readXML(xmlFile);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException("Exception was thrown");
        }
    }

    private void readXML(InputStream xml) throws SAXException, IOException, ParserConfigurationException {
        assert (xml != null) : "xmlFile may not be null";

        final SAXHandler handler = new SAXHandler();
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.newSAXParser().parse(new InputSource(xml), handler);
    }

    //  Needed to delay initialization 
    private interface ISubTags {
        public Set<Tag> getSubTags();
    }

    private interface HistoryKey {}
    /**
     *  Only includes relevant tags.
     */
    private enum Tag implements HistoryKey {
        /**
         *  This tag is nat an actual part of the document.
         */
        ROOT("ROOT",
                new ISubTags() { @Override public Set<Tag> getSubTags() {
                    return EnumSet.of(Tag.MANIFEST); }},
                null,
                NoOpItem.class),
        MANIFEST("manifest",
                new ISubTags() { @Override public Set<Tag> getSubTags() {
                    return EnumSet.of(Tag.APPLICATION); }},
                EnumSet.of(Attr.PACKAGE),
                ManifestItem.class),
        APPLICATION("application", 
                new ISubTags() { @Override public Set<Tag> getSubTags() {
                    return EnumSet.of(Tag.ACTIVITY, Tag.SERVICE, Tag.RECEIVER, Tag.PROVIDER, Tag.ALIAS); }},    // Allowed children..
                Collections.EMPTY_SET,              // Interesting Attributes
                NoOpItem.class),                    // Handler
        ACTIVITY("activity", 
                new ISubTags() { @Override public Set<Tag> getSubTags() {
                    return EnumSet.of(Tag.INTENT); }},
                EnumSet.of(Attr.NAME, Attr.ENABLED, Attr.PROCESS),
                ComponentItem.class),
        ALIAS("activity-alias", 
                new ISubTags() { @Override public Set<Tag> getSubTags() {
                    return EnumSet.of(Tag.INTENT); }},
                EnumSet.of(Attr.ENABLED, Attr.TARGET, Attr.NAME),
                ComponentItem.class),
        SERVICE("service", 
                new ISubTags() { @Override public Set<Tag> getSubTags() {
                    return EnumSet.of(Tag.INTENT); }},
                EnumSet.of(Attr.ENABLED, Attr.NAME, Attr.PROCESS),
                ComponentItem.class),
        RECEIVER("receiver", 
                new ISubTags() { @Override public Set<Tag> getSubTags() {
                      return EnumSet.of(Tag.INTENT); }},
                EnumSet.of(Attr.ENABLED, Attr.NAME, Attr.PROCESS),
                ComponentItem.class),
        PROVIDER("provider", 
                new ISubTags() { @Override public Set<Tag> getSubTags() {
                    return EnumSet.of(Tag.INTENT); }},
                EnumSet.of(Attr.ENABLED, Attr.ORDER, Attr.NAME, Attr.PROCESS),
                ComponentItem.class),
        INTENT("intent-filter",
                new ISubTags() { @Override public Set<Tag> getSubTags() {
                    return EnumSet.of(Tag.ACTION, Tag.DATA); }},
                Collections.EMPTY_SET,
                IntentItem.class),
        ACTION("action", 
                Collections::emptySet,
                EnumSet.of(Attr.NAME),
                FinalItem.class), //(new ITagDweller() {
                    //public Tag getTag() { return Tag.ACTION; }})),
        DATA("data", 
                Collections::emptySet,
                EnumSet.of(Attr.SCHEME, Attr.HOST, Attr.PATH, Attr.MIME),
                FinalItem.class), //(new ITagDweller() {
                    //public Tag getTag() { return Tag.DATA; }})),
        /**
         *  Internal pseudo-tag used for tags in the documents, that have no parser representation.
         */
        UNIMPORTANT("UNIMPORTANT",
                null,
                Collections.EMPTY_SET,
                null);

        private final String tagName;
        private final Set<Attr> relevantAttributes;
        private final ISubTags allowedSubTagsHolder;
        private final ParserItem item;
        private Set<Tag> allowedSubTags;    // Delay init
        private static final Map<String, Tag> reverseMap = new HashMap<>();// HashMapFactory.make(9);
        
        Tag (String tagName, ISubTags allowedSubTags, Set<Attr> relevant, Class<? extends ParserItem> item) {
            this.tagName = tagName;
            this.relevantAttributes = relevant;
            this.allowedSubTagsHolder = allowedSubTags;
            if (item != null) {
                try {
                    this.item = item.newInstance();
                    this.item.setSelf(this);
                } catch (java.lang.InstantiationException e) {
                    e.getCause().printStackTrace();
                    throw new IllegalStateException("InstantiationException was thrown");
                } catch (java.lang.IllegalAccessException e) {
                    e.printStackTrace();
                    if (e.getCause() != null) {
                        e.getCause().printStackTrace();
                    }
                    throw new IllegalStateException("IllegalAccessException was thrown");
                }
            } else {
                this.item = null;
            }
        }

        static {
            for (Tag tag: Tag.values()) {
                reverseMap.put(tag.tagName, tag);
            }
        }

        /**
         *  The class that takes action on this tag.
         */
        public ParserItem getHandler() {
            if (this.item == null) {
                System.err.println("Requested non existing handler for: " + this.toString());
            }
            return this.item;
        }

        /**
         *  The Tags that may appear as a child of this Tag.
         */
        public Set<Tag> getAllowedSubTags() {
            if (this.allowedSubTagsHolder == null) {
                return null;
            } else if (this.allowedSubTags == null) {
               this.allowedSubTags = allowedSubTagsHolder.getSubTags();
            }

            return Collections.unmodifiableSet(this.allowedSubTags);
        }

        /**
         *  The Attributes read in when parsing the Tag.
         *
         *  The read attributes get thrown on a Stack, thus they don't need to be evaluated
         *  in the Tag itself but may also be handled by a parent.
         *
         *  The handling Item has to pop them after it has evaluated them else it gets a big 
         *  mess.
         */
        public Set<Attr> getRelevantAttributes() {
            return Collections.unmodifiableSet(this.relevantAttributes);
        }

        /**
         *  The given Attr is in {@link #getRelevantAttributes()}.
         */
        public boolean isRelevant(Attr attr) {
            return relevantAttributes.contains(attr);
        }

        /**
         *  All Tags in this Enum but UNIMPORTANT are relevant.
         */
        @SuppressWarnings("unused")
        public boolean isRelevant() {
            return (this != Tag.UNIMPORTANT);
        }

        /**
         *  Match the Tag-Name in the XML-File against the one associated to the Enums Tag.
         *
         *  If no Tag in this Enum matches Tag.UNIMPORTANT is returned and the parser will ignore the
         *  tree under this tag.
         *
         *  Matching is case insensitive of course.
         */
        public static Tag fromString(String tag) {
            tag = tag.toLowerCase();
            
            if (reverseMap.containsKey(tag)) {
                return reverseMap.get(tag);
            } else {
                return Tag.UNIMPORTANT;
            }
        }

        /**
         *  The Tag appears in the XML File using this name.
         */
        @SuppressWarnings("unused")
        public String getName() {
            return this.tagName;
        }
    }

    /**
     *  Attributes that may appear in a Tags.Tag.
     *
     *  In order to evaluate a new attribute it has to be added to the Tags it may appear in and 
     *  at least one ParserItem has to be adapted.
     *
     *  Values read for the single Attrs will get pushed to the attributesHistory (in Items).
     */
    private enum Attr implements HistoryKey {
        PACKAGE("package"),
        NAME("name"),
        SCHEME("scheme"),
        HOST("host"),
        PATH("path"),
        ENABLED("enabled"),
        TARGET("targetActivity"),
        PROCESS("process"),
        ORDER("initOrder"),
        MIME("mimeType");

        private final String attrName;
        Attr(String attrName) {
            this.attrName = attrName;
        }

        @SuppressWarnings("unused")
        public boolean isRelevantIn(Tag tag) {
            return tag.isRelevant(this);
        }

        public String getName() {
            return this.attrName;
        }
    }

    /**
     *  Contains the "path" from Tag.ROOT that currently gets evaluated.
     *
     *  On an opening Tag the Tag gets pushed. It get's popped again once it's evaluated. 
     *  That is on the closing Tag or on the closing Tag of a parent: A Item does not remove its 
     *  own Tag from the Stack.
     */
    private static final Stack<Tag> parserStack = new Stack<>();
    
    /**
     *  Contains either Attributes of a child or the evaluation-result of a child-Tag.
     *
     *  The Item that consumes an Attribute has to pop it.
     */
    private static final Map<HistoryKey, Stack<Object>> attributesHistory = new HashMap<>();  // No EnumMap possible :(

    static {
        for (Attr attr : Attr.values()) {
            attributesHistory.put(attr, new Stack<>());
        }
        for (Tag tag : Tag.values()) {
            attributesHistory.put(tag, new Stack<>());
        }
    }
    
    /**
     *  Handling of a Tag.
     *
     *  Does (if not overridden) all the needed Push- and Pop-Operations. Items may however choose to leave some
     *  stuff on the Stack. In this case this data has to be popped by the handling parent, or the Tag may only
     *  occur once or bad things will happen.
     *
     *  _CAUTION_: This will be instantiated by an Enum, so if you write local Fields you will get surprising 
     *  results! You should mark all of them as final to be sure.
     */
    private static abstract class ParserItem {
        protected Tag self;
        /**
         *  Set the Tag this ParserItem-Instance is an Handler for.
         *
         *  This may only be set once!
         */
        public void setSelf(Tag self) {
            if (this.self != null) {
                throw new IllegalStateException("Self can only be set once!");
            }
            this.self = self;
        }
        
        public ParserItem() {
        }
        
        /**
         *  Remember attributes to the tag. 
         *
         *  The read attributes will be pushed to the attributesHistory.
         *
         *  Leave Parser-Stack alone! This is called by SAXHandler only!
         */    
        public void enter(Attributes saxAttrs) {
            for (Attr relevant : self.getRelevantAttributes()) {
                String attr = saxAttrs.getValue(relevant.getName());
                if (attr == null) {
                    attr = saxAttrs.getValue("android:" + relevant.getName());
                }

                attributesHistory.get(relevant).push(attr);
                logger.debug("Pushing '{}' for {} in {}", attr, relevant, self);
                // if there is no such value in saxAttrs it returns null 
            }
        }
        /**
         *  Remove all Attributes generated by self and self itself.
         *
         *  This is called by the consuming ParserItem.
         */
        public void popAttributes() {
             for (Attr relevant : self.getRelevantAttributes()) {
                 try {
                    logger.debug("Popping {} of value {} in {}", relevant, attributesHistory.get(relevant).peek(), self);
                    attributesHistory.get(relevant).pop();
                 }  catch (java.util.EmptyStackException e) {
                    System.err.println(self + " failed to pop " + relevant);
                    throw e;
                 }
             }
             if (attributesHistory.containsKey(self) && attributesHistory.get(self) != null &&
                     (! attributesHistory.get(self).isEmpty())) {
                 try {
                     attributesHistory.get(self).pop();
                 } catch (java.util.EmptyStackException e) {
                    System.err.println("The Stack for " + self + " was Empty when trying to pop");
                    throw e;
                 }
             }
        }

        /**
         *  Consume sub-items on the stack.
         *
         *  Do this by popping them, but leave self on the stack!
         *  For each Item popped call its popAttributes()!
         */
        public void leave() {
            while (parserStack.peek() != self) {
                final Set<Tag> allowedSubTags = self.getAllowedSubTags();
                Tag subTag = parserStack.pop();
                if (allowedSubTags.contains(subTag)) {
                    if (subTag.getHandler() == null) {
                        throw new IllegalArgumentException("The SubTag " + subTag.toString() + " has no handler!");
                    }
                    subTag.getHandler().popAttributes(); // hmmm....

                    logger.debug("New Stack: {}", parserStack);
                    //parserStack.pop();
                } else {
                    throw new IllegalStateException(subTag + " is not allowed as sub-tag of " + self + " in Context:\n\t" + parserStack);
                }
            }
        }
    }

    /**
     *  An ParserItem that contains no sub-tags.
     *
     *  You can use it directly if you don't intend to do any computation on this Tag but remember its
     *  Attributes.
     */
    private static class FinalItem extends ParserItem {
        @Override
        public void leave() {
            final Set<Tag> subs = self.getAllowedSubTags();
            if (!((subs == null) || subs.isEmpty())) {
                throw new IllegalArgumentException("FinalItem can not be applied to " + self + " as it contains sub-tags: " + 
                        self.getAllowedSubTags());
            }

            if (parserStack.peek() != self) {
                throw new IllegalStateException("Topstack is not " + self + " which is disallowed for a FinalItem!\n" +
                        "This is most certainly caused by an implementation mistake on a ParserItem. Stack is:\n\t" + parserStack);
            }
        }
    }

    /**
     *  Only extracts Attributes.
     *
     *  It's like FinalItem but may contain sub-tags.
     */
    private static class NoOpItem extends ParserItem {
    }

    /**
     *  The root-element of an AndroidManifest contains the package.
     */
    private static class ManifestItem extends ParserItem {
        @Override
        public void enter(Attributes saxAttrs) {
            super.enter(saxAttrs);
            AndroidEntryPointManager.MANAGER.setPackage((String) attributesHistory.get(Attr.PACKAGE).peek()); 
        }
    }

    /**
     *  Read the specification of an Intent from AndroidManifest.
     *
     *  TODO: Handle the URI
     */
    private static class IntentItem extends ParserItem {
        @Override
        public void leave() {
            Set<Tag> allowedTags = EnumSet.copyOf(self.getAllowedSubTags());
            Set<String> urls = new HashSet<>();
            Set<String> names = new HashSet<>();
            while (parserStack.peek() != self) {
                Tag current = parserStack.pop();
                if (allowedTags.contains(current)) {
                    if (current == Tag.ACTION) {
                        Object oName = attributesHistory.get(Attr.NAME).peek();
                        if (oName == null) {
                             throw new IllegalStateException("The currently parsed Action did not leave the required 'name' Attribute" +
                                     " on the Stack! Attributes-Stack for name is: " + attributesHistory.get(Attr.NAME));
                        } else if (oName instanceof String) {
                            names.add((String) oName);
                        } else {
                            throw new IllegalStateException("Unexpected Attribute type for name: " + oName.getClass().toString());
                        }
                    } else if (current == Tag.DATA) {
                        Object oUrl = attributesHistory.get(Attr.SCHEME).peek();
                        if (oUrl == null) {
                            // TODO
                        } else if (oUrl instanceof String) {
                            urls.add((String) oUrl);
                        } else {
                            throw new IllegalStateException("Unexpected Attribute type for name: " + oUrl.getClass().toString());
                        }
                    } else {
                        throw new IllegalStateException("Error in parser implementation");
                    }
                    current.getHandler().popAttributes();
                } else {
                    throw new IllegalStateException("In INTENT: Tag " + current + " not allowed in Context " + parserStack + "\n\t"+
                            "Allowed Tags: " + allowedTags);
                }
            }

            /*
            // Pushing intent...
            final String pack;
            if ((attributesHistory.get(Attr.PACKAGE) != null ) && (!(attributesHistory.get(Attr.PACKAGE).isEmpty()))) {
                pack = (String) attributesHistory.get(Attr.PACKAGE).peek();
            } else {
                logger.warn("Empty Package {}", attributesHistory.get(Attr.PACKAGE).peek());
                pack = null;
            }
            */

            if (!names.isEmpty()) {
                for (String name : names) {
                    if (urls.isEmpty()) urls.add(null);
                    for (String url : urls) {
                        logger.info("New Intent ({}, {})", name, url);
                        final Intent intent = AndroidSettingFactory.intent(name, url);
                        attributesHistory.get(self).push(intent);
                    }
            }
            } else {
            /**
             *  Previously, an exception was thrown but in fact there is no need to crash here.
             *  Actions are required, but if there is no action in a particular intent-filter,
             *  then no intent will pass the intent filter.
             *  See also http://developer.android.com/guide/topics/manifest/action-element.html
             *  So we'll just issue a warning and continue happily with our work...
             */
            logger.warn("specified intent without action - this means that no intents will pass the filter...");
            }
        }
    }

    private static class ComponentItem extends ParserItem {
        @Override
         public void leave() {
            final Set<Tag> allowedTags = self.getAllowedSubTags();
            final Set<Intent> overrideTargets = new HashSet<>(); 

            while (parserStack.peek() != self) {
                Tag current = parserStack.pop();
                if (allowedTags.contains(current)) {
                    if (current == Tag.INTENT) {
                    // do not expect item at top of stack if no intent was produced
                    if (attributesHistory.get(Tag.INTENT).isEmpty()) continue;
                        Object oIntent = attributesHistory.get(Tag.INTENT).peek();
                        if (oIntent == null) {
                             throw new IllegalStateException("The currently parsed Intent did not push a Valid intent to the " +
                                     "Stack! Attributes-Stack for name is: " + attributesHistory.get(Attr.NAME));
                        } else if (oIntent instanceof Intent) {
                            overrideTargets.add( (Intent) oIntent );
                        } else {
                            throw new IllegalStateException("Unexpected Attribute type for Intent: " + oIntent.getClass().toString());
                        }
                    } else {
                        throw new IllegalStateException("Error in parser implementation");
                    }
                    current.getHandler().popAttributes();
                } else {
                    throw new IllegalStateException("In " + self + ": Tag " + current + " not allowed in Context " + parserStack + "\n\t"+
                            "Allowed Tags: " + allowedTags);
                }
            }

            // Generating Intent for this...
            final String pack;
            if ((attributesHistory.get(Attr.PACKAGE) != null ) && (!(attributesHistory.get(Attr.PACKAGE).isEmpty()))) {
                pack = (String) attributesHistory.get(Attr.PACKAGE).peek();
            } else {
                logger.warn("Empty Package {}", attributesHistory.get(Attr.PACKAGE).peek());
                pack = null;
            }

            final String name;
            if (self == Tag.ALIAS) {
                name =  (String) attributesHistory.get(Attr.TARGET).peek(); // TODO: Verify type!
            } else {
                name = (String) attributesHistory.get(Attr.NAME).peek(); // TODO: Verify type!
            }
            final Intent intent = AndroidSettingFactory.intent(pack, name, null);

            logger.info("\tRegister: {}", intent);
            AndroidEntryPointManager.MANAGER.registerIntent(intent);
            for (Intent ovr: overrideTargets) {
                logger.info("\tOverride: {} --> {}", ovr, intent);
                if (ovr.equals(intent)) {
                    AndroidEntryPointManager.MANAGER.registerIntent(intent);
                } else {
                    AndroidEntryPointManager.MANAGER.setOverride(ovr, intent);
                }
            }
        }
    }

    private class SAXHandler extends DefaultHandler {
        private int unimportantDepth = 0;

        public SAXHandler() {
            super();
            parserStack.push(Tag.ROOT);
        }

        @Override
        public void startElement(String uri, String name, String qName, Attributes attrs) {
            Tag tag = Tag.fromString(qName);
            if ((tag == Tag.UNIMPORTANT) || (unimportantDepth > 0)) {
                unimportantDepth++;
            } else {
                logger.debug("Handling {} made from {}", tag, qName);
                
                final ParserItem handler = tag.getHandler();
                if (handler != null) {
                    handler.enter(attrs);
                }
                parserStack.push(tag);

            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            if (unimportantDepth > 0) {
                unimportantDepth--;
            } else {
                final Tag tag = Tag.fromString(qName);
                final ParserItem handler = tag.getHandler();
                if (handler != null) {
                    handler.leave();
                }
            }
        }
    }
}
