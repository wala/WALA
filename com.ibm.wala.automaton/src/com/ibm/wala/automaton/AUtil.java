/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.automaton;

import java.io.*;
import java.util.*;

public class AUtil {
    static public Collection collection(Class klass, Object objs[]) throws InstantiationException, IllegalAccessException {
        Collection c = (Collection) klass.newInstance();
        for (int i = 0; i < objs.length; i++) {
            c.add(objs[i]);
        }
        return c;
    }

    static public Collection collection(Class klass, Iterator iter) throws InstantiationException, IllegalAccessException {
        Collection c = (Collection) klass.newInstance();
        while (iter.hasNext()) {
            c.add(iter.next());
        }
        return c;
    }
    
    static public List list(Object objs[]) {
        try {
            return (List) collection(ArrayList.class, objs);
        } catch (InstantiationException e) {
            e.printStackTrace();
            return null;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    static public List list(Iterator i) {
        try {
            return (List) collection(ArrayList.class, i);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    static public Set set(Object objs[]) {
        try {
            return (Set) collection(HashSet.class, objs);
        } catch (InstantiationException e) {
            e.printStackTrace();
            return null;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    static public Set set(Iterator i) {
        try {
            return (Set) collection(HashSet.class, i);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    static public Map map(Object keys[], Object vals[]) {
        Map m = new HashMap();
        for (int i = 0; i < keys.length; i++) {
            if (i < vals.length) {
                m.put(keys[i], vals[i]);
            }
            else {
                m.put(keys[i], null);
            }
        }
        return m;
    }
    
    static public Set allOrder(Set s) {
        return allOrder(new ArrayList(s));
    }
    
    static public Set allOrder(List l) {
        if (l.isEmpty()) {
            return new HashSet();
        }
        else {
            Object hd = l.get(0);
            l.remove(0);
            Set comb = insertAll(hd, allOrder(l));
            return comb;
        }
    }
    
    static private Set insert(Object o, List l) {
        HashSet comb = new HashSet();
        int size = l.size();
        for (int i = 0; i <= size; i++) {
            ArrayList cl = new ArrayList(l);
            cl.add(i, o);
            comb.add(cl);
        }
        return comb;
    }
    
    static private Set insertAll(Object o, Set s) {
        HashSet comb = new HashSet();
        if (s.isEmpty()) {
            comb.addAll(insert(o, new ArrayList()));
        }
        else {
            for (Iterator i = s.iterator(); i.hasNext(); ) {
                List l = (List) i.next();
                comb.addAll(insert(o, l));
            }
        }
        return comb;
    }
    
    static public List sort(Collection collection) {
        List l = new ArrayList(collection);
        Collections.sort(l, new Comparator(){
            public int compare(Object o1, Object o2) {
                return o1.toString().compareTo(o2.toString());
            }});
        return l;
    }

    public interface IElementMapper {
        Object map(Object obj);
    }
    
    static public List collect(Collection collection, IElementMapper mapper) {
        List result = new ArrayList();
        for (Iterator i = collection.iterator(); i.hasNext(); ) {
            Object obj = i.next();
            obj = mapper.map(obj);
            result.add(obj);
        }
        return result;
    }
    
    public interface IElementSelector {
        boolean selected(Object obj);
    }
    
    static public List select(Collection collection, IElementSelector selector) {
        List result = new ArrayList();
        for (Iterator i = collection.iterator(); i.hasNext(); ) {
            Object obj = i.next();
            if (selector.selected(obj)) {
                result.add(obj);
            }
        }
        return result;
    }

    /**
     * create an unique string that is not contained by names.
     * @param prefix
     * @param names        an unique string is added
     * @param start
     * @return
     */
    static public String createUniqueName(final String prefix, final Collection names, int start) {
        int i = start;
        String name = ((prefix==null) ? "n" : prefix) + Integer.toString(i);
        while (names.contains(name)) {
            i ++;
            name = ((prefix==null) ? "n" : prefix) + Integer.toString(i);
        }
        names.add(name);
        return name;
    }
    
    static public String createUniqueName(final String prefix, final Collection names) {
        return createUniqueName(prefix, names, 1);
    }

    static public String lineSeparator = (String) java.security.AccessController.doPrivileged(
            new sun.security.action.GetPropertyAction("line.separator"));
 
    static public String prettyFormat(Object obj) {
        return prettyFormat(obj, 4);
    }
    
    static public String prettyFormat(Object obj, int indent) {
        return prettyFormat(obj, "", indent);
    }
    
    static public String prettyFormat(Object obj, String prefix, int indent) {
        StringBuffer buff = new StringBuffer();
        prettyFormat(obj.toString().toCharArray(), 0, buff, prefix, indent);
        return buff.toString();
    }
    
    static public int prettyFormat(char cs[], int index, StringBuffer buff, String prefix, int indent) {
        int i;
        buff.append(prefix);
        for (i = index; i < cs.length; i++) {
            char c = cs[i];
            switch (c) {
            case '{':
                buff.append(c);
                buff.append(lineSeparator);
                while(i+1<cs.length && Character.isSpaceChar(cs[i+1])){
                    i++;
                }
                String prefix2 = prefix;
                for (int j=0; j<indent; j++){ prefix2 = prefix2 + " "; }
                i = prettyFormat(cs, i+1, buff, prefix2, indent);
                break;
            case '}':
                prefix = prefix.substring(0, prefix.length()-indent);
                buff.append(lineSeparator + prefix);
                while(i+1<cs.length && Character.isSpaceChar(cs[i+1])){
                    i++;
                }
                buff.append(c);
                return i;
            case ';':
            case ',':
                buff.append(c);
                buff.append(lineSeparator + prefix);
                while(i+1<cs.length && Character.isSpaceChar(cs[i+1])){
                    i++;
                }
                break;
            case ' ':
            case '\t':
                buff.append(' ');
                break;
            default:
                buff.append(c);
            }
        }
        return i;
    }
    
    static public Object deepCopy(Object obj) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();        
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);        
            ObjectInputStream ois = new ObjectInputStream(        
               new ByteArrayInputStream(bos.toByteArray()));        
            return ois.readObject();
        } catch (IOException e) {
            throw(new RuntimeException(e));
        } catch (ClassNotFoundException e) {
            throw(new RuntimeException(e));
        }
    }
}
