/*
 *
 * Copyright (c) 2009-2012,
 *
 *  Jonathan Bardin     <astrosus@gmail.com>
 *  Steve Suh           <suhsteve@gmail.com>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. The names of the contributors may not be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */

package com.ibm.wala.dalvik.classLoader;

import static org.jf.dexlib.ItemType.TYPE_CLASS_DEF_ITEM;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.jf.dexlib.ClassDefItem;
import org.jf.dexlib.DexFile;
import org.jf.dexlib.Section;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;

/**
 * A module which is a wrapper around .dex and .apk file.
 *
 * @author barjo
 */
public class DexFileModule implements Module {
	private static final Logger logger = LoggerFactory.getLogger(DexFileModule.class);

    private final DexFile dexfile;
    private final Collection<ModuleEntry> entries;

    /**
     * @param f
     *            the .dex or .apk file
     * @throws IllegalArgumentException
     */
    public DexFileModule(File f) throws IllegalArgumentException {    	
        try {
            dexfile = new DexFile(f);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        // create ModuleEntries from ClassDefItem
        entries = new HashSet<ModuleEntry>();

        Section<ClassDefItem> cldeff = dexfile
                .getSectionForType(TYPE_CLASS_DEF_ITEM);

        for (ClassDefItem cdefitems : cldeff.getItems()) {
            logger.debug("DexFileModule adding class: " + cdefitems.getConciseIdentity());
            entries.add(new DexModuleEntry(cdefitems));
        }
    }

    /**
     * @return The DexFile associated to this module.
     */
    public DexFile getDexFile() {
        return dexfile;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wala.classLoader.Module#getEntries()
     */
    public Iterator<ModuleEntry> getEntries() {
        return entries.iterator();
    }

}
