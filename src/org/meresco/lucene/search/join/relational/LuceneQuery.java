/* begin license *
 *
 * "Meresco Lucene" is a set of components and tools to integrate Lucene (based on PyLucene) into Meresco
 *
 * Copyright (C) 2016 Seecr (Seek You Too B.V.) http://seecr.nl
 *
 * This file is part of "Meresco Lucene"
 *
 * "Meresco Lucene" is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * "Meresco Lucene" is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with "Meresco Lucene"; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * end license */

package org.meresco.lucene.search.join.relational;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.meresco.lucene.Lucene;
import org.meresco.lucene.queries.KeyFilter;
import org.meresco.lucene.search.join.KeySuperCollector;


public class LuceneQuery implements RelationalQuery {
    String core;
    String collectKeyName;
    String filterKeyName;
    Query q;
    float boost;

    public LuceneQuery(String core, String keyName, Query q) {
        this(core, keyName, keyName, q);
    }

    public LuceneQuery(String core, String collectKeyName, String filterKeyName, Query q) {
        this(core, collectKeyName, filterKeyName, q, 1.0f);
    }

    public LuceneQuery(String core, String collectKeyName, String filterKeyName, Query q, float boost) {
        this.core = core;
        this.collectKeyName = collectKeyName;
        this.filterKeyName = filterKeyName;
        this.q = q;
        this.boost = boost;  // ignored so far
    }

    @Override
    public IntermediateResult collectKeys(Map<String, Lucene> lucenes) {
        return this.asExecutable().collectKeys(lucenes);
    }

    @Override
    public ExecutableRelationalQuery asExecutable() {
        return new ExecutableLuceneQuery(this);
    }


    @Override
    public String toString() {
        return "LuceneQuery(\"" + this.core + "\", \"" + this.collectKeyName + "\", \"" + this.filterKeyName + "\", " + this.q + ")";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Float.floatToIntBits(boost);
        result = prime * result + ((collectKeyName == null) ? 0 : collectKeyName.hashCode());
        result = prime * result + ((core == null) ? 0 : core.hashCode());
        result = prime * result + ((filterKeyName == null) ? 0 : filterKeyName.hashCode());
        result = prime * result + ((q == null) ? 0 : q.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        LuceneQuery other = (LuceneQuery) obj;
        if (!collectKeyName.equals(other.collectKeyName)) {
            return false;
        }
        if (!core.equals(other.core)) {
            return false;
        }
        if (!filterKeyName.equals(other.filterKeyName)) {
            return false;
        }
        if (!q.equals(other.q)) {
            return false;
        }
        if (Float.floatToIntBits(boost) != Float.floatToIntBits(other.boost)) {
            return false;
        }
        return true;
    }


    static class ExecutableLuceneQuery implements ExecutableRelationalQuery {
        LuceneQuery luceneQuery;
        Query q;

        public ExecutableLuceneQuery(LuceneQuery luceneQuery) {
            this.luceneQuery = luceneQuery;
            this.q = luceneQuery.q;
        }

        @Override
        public IntermediateResult collectKeys(Map<String, Lucene> lucenes) {
    //        System.out.println("collectKeys " + this);
            KeySuperCollector keyCollector = new KeySuperCollector(this.luceneQuery.collectKeyName);
            try {
    //            System.out.println("search " + this.q);
                lucenes.get(this.luceneQuery.core).search(this.q, keyCollector);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            IntermediateResult result = new IntermediateResult(keyCollector.getCollectedKeys());
    //        System.out.println("result: " + result);
            return result;
        }

        @Override
        public void invert() {
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            builder.add(new MatchAllDocsQuery(), BooleanClause.Occur.MUST);
            builder.add(this.q, BooleanClause.Occur.MUST_NOT);
            this.q = builder.build();
        }

        @Override
        public void filter(IntermediateResult intermediateResult) {
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            builder.add(this.q, BooleanClause.Occur.MUST);
            try {
                builder.add(new KeyFilter(intermediateResult.getBitSet(), this.luceneQuery.filterKeyName, intermediateResult.inverted), BooleanClause.Occur.MUST);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            this.q = builder.build();
        }

        @Override
        public void union(IntermediateResult intermediateResult) {
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            try {
                builder.add(new KeyFilter(intermediateResult.getBitSet(), this.luceneQuery.filterKeyName, intermediateResult.inverted), BooleanClause.Occur.SHOULD);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            builder.add(this.q, BooleanClause.Occur.SHOULD);
            this.q = builder.build();
        }

        @Override
        public ExecutableRelationalQuery asExecutable() {
            return this;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "@" + System.identityHashCode(this) + "(" + this.luceneQuery + ")";
        }
    }
}