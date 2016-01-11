/* begin license *
 *
 * "Meresco Lucene" is a set of components and tools to integrate Lucene (based on PyLucene) into Meresco
 *
 * Copyright (C) 2015 Koninklijke Bibliotheek (KB) http://www.kb.nl
 * Copyright (C) 2015 Seecr (Seek You Too B.V.) http://seecr.nl
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

package org.meresco.lucene;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.ArrayList;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.facet.FacetsConfig.DimConfig;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.junit.Test;
import org.meresco.lucene.analysis.MerescoDutchStemmingAnalyzer;
import org.meresco.lucene.analysis.MerescoStandardAnalyzer;
import org.meresco.lucene.search.TermFrequencySimilarity;

public class LuceneSettingsTest {

    @Test
    public void testSettingsAsJson() {
        LuceneSettings settings = new LuceneSettings();
        assertEquals("{"
                + "\"similarity\":\"BM25(k1=1.2,b=0.75)\","
                + "\"maxMergeAtOnce\":2,"
                + "\"segmentsPerTier\":8.0,"
                + "\"lruTaxonomyWriterCacheSize\":4000,"
                + "\"numberOfConcurrentTasks\":6,"
                + "\"commitCount\":100000,"
                + "\"commitTimeout\":10,"
                + "\"clusteringEps\":0.4,"
                + "\"clusteringMinPoints\":1,"
                + "\"clusterMoreRecords\":100"
            + "}", settings.asJson().toString());
    }


    @Test
    public void testSettingsFromJson() throws Exception {
        LuceneSettings settings = new LuceneSettings();
        String json = "{\"commitCount\": 1, \"commitTimeout\": 1, \"lruTaxonomyWriterCacheSize\": 1, \"maxMergeAtOnce\": 1, \"segmentsPerTier\": 1.0, \"numberOfConcurrentTasks\": 1}";

        settings.updateSettings(new StringReader(json));
        assertEquals(1, settings.commitCount);
        assertEquals(1, settings.commitTimeout);
        assertEquals(1, settings.lruTaxonomyWriterCacheSize);
        assertEquals(1, settings.maxMergeAtOnce);
        assertEquals(1.0, settings.segmentsPerTier, 0);
        assertEquals(1, settings.commitTimeout);
    }

    @SuppressWarnings("serial")
    @Test
    public void testMerescoDutchStemmingAnalyzer() throws Exception {
        LuceneSettings settings = new LuceneSettings();
        String json = "{\"analyzer\": {\"type\": \"MerescoDutchStemmingAnalyzer\", \"fields\": [\"field0\", \"field1\"]}}";

        settings.updateSettings(new StringReader(json));
        assertEquals(MerescoDutchStemmingAnalyzer.class, settings.analyzer.getClass());

        assertEquals(new ArrayList<String>() {{ add("katten"); add("kat");}}, MerescoStandardAnalyzer.readTokenStream(settings.analyzer.tokenStream("field0", "katten")));
        assertEquals(new ArrayList<String>() {{ add("katten"); add("kat");}}, MerescoStandardAnalyzer.readTokenStream(settings.analyzer.tokenStream("field1", "katten")));
        assertEquals(new ArrayList<String>() {{ add("katten");}}, MerescoStandardAnalyzer.readTokenStream(settings.analyzer.tokenStream("field2", "katten")));
    }

    @Test
    public void testMerescoStandardAnalyzer() throws Exception {
        LuceneSettings settings = new LuceneSettings();
        String json = "{\"analyzer\": {\"type\": \"MerescoStandardAnalyzer\", \"fields\": [\"field0\", \"field1\"]}}";

        settings.updateSettings(new StringReader(json));
        assertEquals(MerescoStandardAnalyzer.class, settings.analyzer.getClass());
    }
    
    @Test
    public void testWhitespaceAnalyzer() throws Exception {
        LuceneSettings settings = new LuceneSettings();
        String json = "{\"analyzer\": {\"type\": \"WhitespaceAnalyzer\"}}";

        settings.updateSettings(new StringReader(json));
        assertEquals(WhitespaceAnalyzer.class, settings.analyzer.getClass());
    }

    @Test
    public void testBM25Similarity() throws Exception {
        LuceneSettings settings = new LuceneSettings();
        String json = "{\"similarity\": {\"type\": \"BM25Similarity\"}}";

        settings.updateSettings(new StringReader(json));
        assertEquals(BM25Similarity.class, settings.similarity.getClass());
        assertEquals(0.75f, ((BM25Similarity) settings.similarity).getB(), 0);
        assertEquals(1.2f, ((BM25Similarity) settings.similarity).getK1(), 0);
    }

    @Test
    public void testBM25SimilarityWithKAndB() throws Exception {
        LuceneSettings settings = new LuceneSettings();
        String json = "{\"similarity\": {\"type\": \"BM25Similarity\", \"k1\": 1.0, \"b\": 2.0}}";

        settings.updateSettings(new StringReader(json));
        assertEquals(BM25Similarity.class, settings.similarity.getClass());
        assertEquals(2.0f, ((BM25Similarity) settings.similarity).getB(), 0);
        assertEquals(1.0f, ((BM25Similarity) settings.similarity).getK1(), 0);
    }

    @Test
    public void testTermFrequencySimilarity() throws Exception {
        LuceneSettings settings = new LuceneSettings();
        String json = "{\"similarity\": {\"type\": \"TermFrequencySimilarity\"}}";

        settings.updateSettings(new StringReader(json));
        assertEquals(TermFrequencySimilarity.class, settings.similarity.getClass());
    }

    @Test
    public void testDrilldownFields() throws Exception {
        LuceneSettings settings = new LuceneSettings();
        String json = "{\"drilldownFields\": [" +
                    "{\"dim\": \"field0\", \"hierarchical\": true, \"multiValued\": false}," +
                    "{\"dim\": \"field1\", \"hierarchical\": true, \"multiValued\": true, \"fieldname\": \"$facets_2\"}," +
                    "{\"dim\": \"field2\", \"hierarchical\": false, \"multiValued\": false, \"fieldname\": null}" +
                "]}";

        settings.updateSettings(new StringReader(json));
        DimConfig field0 = settings.facetsConfig.getDimConfig("field0");
        assertTrue(field0.hierarchical);
        assertFalse(field0.multiValued);
        assertEquals("$facets", field0.indexFieldName);

        DimConfig field1 = settings.facetsConfig.getDimConfig("field1");
        assertTrue(field1.hierarchical);
        assertTrue(field1.multiValued);
        assertEquals("$facets_2", field1.indexFieldName);

        DimConfig field2 = settings.facetsConfig.getDimConfig("field2");
        assertFalse(field2.hierarchical);
        assertFalse(field2.multiValued);
        assertEquals("$facets", field2.indexFieldName);
    }
}