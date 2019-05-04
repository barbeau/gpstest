// Copyright 2009 Google Inc. From StarDroid.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.android.gpstest.ar;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Given a set of strings such as search terms, this class allows you to search
 * for that string by prefix.
 *
 * @author John Taylor
 */
public class PrefixStore {

    static private class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();

        // we need to store the originals to support insensitive case searching
        Set<String> results = new HashSet<>();
    }

    private TrieNode root = new TrieNode();

    private static final Set<String> EMPTY_SET = Collections.unmodifiableSet(new HashSet<String>());

    /**
     * Search for any queries matching this prefix.  Note that the prefix is
     * case-independent.
     * <p>
     * TODO(@tcao) refactor this API. Search should return a relevance ranked list.
     */
    public Set<String> queryByPrefix(String prefix) {
        prefix = prefix.toLowerCase();
        TrieNode n = root;
        for (int i = 0; i < prefix.length(); i++) {
            TrieNode c = n.children.get(prefix.charAt(i));
            if (c == null) {
                return EMPTY_SET;
            }
            n = c;
        }
        Set<String> coll = new HashSet<String>();
        collect(n, coll);
        return coll;
    }

    private void collect(TrieNode n, Collection<String> coll) {
        coll.addAll(n.results);
        for (Character ch : n.children.keySet()) {
            collect(n.children.get(ch), coll);
        }
    }

    /**
     * Put a new string in the store.
     */
    public void add(String string) {
        TrieNode n = root;
        String lower = string.toLowerCase();
        for (int i = 0; i < lower.length(); i++) {
            TrieNode c = n.children.get(lower.charAt(i));
            if (c == null) {
                c = new TrieNode();
                n.children.put(lower.charAt(i), c);
            }
            n = c;
        }
        n.results.add(string);
    }

    /**
     * Put a whole load of objects in the store at once.
     *
     * @param strings a collection of strings.
     */
    public void addAll(Collection<String> strings) {
        for (String string : strings) {
            add(string);
        }
    }
}
