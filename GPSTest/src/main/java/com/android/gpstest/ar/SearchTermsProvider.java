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

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.android.gpstest.util.MiscUtils;

import java.util.Set;

/**
 * Provides search suggestions for a list of words and their definitions.
 */
public class SearchTermsProvider extends ContentProvider {
    public static class SearchTerm {
        public String origin;
        public String query;

        public SearchTerm(String query, String origin) {
            this.query = query;
            this.origin = origin;
        }
    }

    private static final String TAG = MiscUtils.getTag(SearchTermsProvider.class);
    public static String AUTHORITY = "com.google.android.stardroid.searchterms";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    private static final int SEARCH_SUGGEST = 0;
    private static final UriMatcher uriMatcher = buildUriMatcher();
    LayerManager layerManager;

    /**
     * The columns we'll include in our search suggestions.
     */
    private static final String[] COLUMNS = {"_id", // must include this column
            SearchManager.SUGGEST_COLUMN_QUERY,
            SearchManager.SUGGEST_COLUMN_TEXT_1,
            SearchManager.SUGGEST_COLUMN_TEXT_2};

    /**
     * Sets up a uri matcher for search suggestion and shortcut refresh queries.
     */
    private static UriMatcher buildUriMatcher() {
        UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);
        return matcher;
    }

    @Override
    public boolean onCreate() {
        maybeInjectMe();
        return true;
    }

    private boolean alreadyInjected;

    private boolean maybeInjectMe() {
//    // Ugh.  Android's separation of content providers from their owning apps makes this
//    // almost impossible.  TODO(jontayler): revisit and see if we can make this less
//    // nasty.
//    if (alreadyInjected) {
//      return true;
//    }
//    Context appContext = getContext().getApplicationContext();
//    if (!(appContext instanceof StardroidApplication)) {
//      return false;
//    }
//    ApplicationComponent component = ((StardroidApplication) appContext).getApplicationComponent();
//    if (component == null) {
//      return false;
//    }
//    component.inject(this);
//    alreadyInjected = true;
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        Log.d(TAG, "Got query for " + uri);
        if (!maybeInjectMe()) {
            return null;
        }
        ;
        if (!TextUtils.isEmpty(selection)) {
            throw new IllegalArgumentException("selection not allowed for " + uri);
        }
        if (selectionArgs != null && selectionArgs.length != 0) {
            throw new IllegalArgumentException("selectionArgs not allowed for " + uri);
        }
        if (!TextUtils.isEmpty(sortOrder)) {
            throw new IllegalArgumentException("sortOrder not allowed for " + uri);
        }
        switch (uriMatcher.match(uri)) {
            case SEARCH_SUGGEST:
                String query = null;
                if (uri.getPathSegments().size() > 1) {
                    query = uri.getLastPathSegment();
                }
                Log.d(TAG, "Got suggestions query for " + query);
                return getSuggestions(query);
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
        }
    }

    private Cursor getSuggestions(String query) {
        MatrixCursor cursor = new MatrixCursor(COLUMNS);
        if (query == null) {
            return cursor;
        }
        Set<SearchTerm> results = layerManager.getObjectNamesMatchingPrefix(query);
        Log.d("SearchTermsProvider", "Got results n=" + results.size());
        for (SearchTerm result : results) {
            cursor.addRow(columnValuesOfSuggestion(result));
        }
        return cursor;
    }

    private static int s = 0;

    private Object[] columnValuesOfSuggestion(SearchTerm suggestion) {
        return new String[]{Integer.toString(s++), // _id
                suggestion.query, // query
                suggestion.query, // text1
                suggestion.origin, // text2
        };
    }

    /**
     * All queries for this provider are for the search suggestion mime type.
     */
    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case SEARCH_SUGGEST:
                return SearchManager.SUGGEST_MIME_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }
}
