package org.scandroid;
/*
 *
 * Copyright (c) 2009-2012,
 *
 *  Adam Fuchs          <afuchs@cs.umd.edu>
 *  Avik Chaudhuri      <avik@cs.umd.edu>
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map.Entry;


public class Permissions {

    private HashMap<String, String> readPerms = new HashMap<String, String>();
    private HashMap<String, String> writePerms = new HashMap<String, String>();

    public Permissions() {
        readPerms.put("content://some.authority1", "READ1");
        writePerms.put("content://some.authority1", "WRITE1");
        readPerms.put("content://some.other.authority1", "READ2");
        writePerms.put("content://some.other.authority1", "WRITE2");
        readPerms.put("content://some.authority2", "READ3");
        writePerms.put("content://some.authority2", "WRITE3");
        readPerms.put("content://some.other.authority2", "READ4");
        writePerms.put("content://some.other.authority2", "WRITE4");
        readPerms.put("content://some.authority3", "READ5");
        writePerms.put("content://some.authority3", "WRITE5");
        readPerms.put("content://some.other.authority3", "READ6");
        writePerms.put("content://some.other.authority3", "WRITE6");
    }

    public static Permissions load(Set<String> manifestFilenames)
    {
        return new Permissions();
    }

    public HashSet<String> readPerms(String uri) {
        HashSet<String> perms = new HashSet<String>();
        for (Entry<String,String> e: readPerms.entrySet()) {
            if (uri.startsWith(e.getKey())) perms.add(e.getValue());
        }
        return perms;
    }

    public HashSet<String> writePerms(String uri) {
        HashSet<String> perms = new HashSet<String>();
        for (Entry<String,String> e: writePerms.entrySet()) {
            if (uri.startsWith(e.getKey())) perms.add(e.getValue());
        }
        return perms;
    }
}
