package org.meresco.lucene;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;

public class QueryStringToQuery {
    
    public Query query;
    public List<FacetRequest> facets;
    public int start;
    public int stop;
    public Sort sort;

    public QueryStringToQuery(Reader queryReader) {
        JsonObject object = Json.createReader(queryReader).readObject();
        this.query = convertToQuery(object.getJsonObject("query"));
        this.facets = convertToFacets(object.getJsonArray("facets"));
        this.start = object.getInt("start", 0);
        this.stop = object.getInt("stop", 10);
        this.sort = convertToSort(object.getJsonArray("sortKeys"));
    }
    
    private Sort convertToSort(JsonArray sortKeys) {
        if (sortKeys == null)
            return null;
        SortField[] sortFields = new SortField[sortKeys.size()];
        for (int i = 0; i < sortKeys.size(); i++) {
            JsonObject sortKey = sortKeys.getJsonObject(i);
            String sortBy = sortKey.getString("sortBy");
            boolean sortDescending = sortKey.getBoolean("sortDescending", false);
            SortField field;
            if (sortBy.equals("score"))
                field = new SortField(null, SortField.Type.SCORE, !sortDescending);
            else
                field = new SortField(sortBy, typeForSortField(sortKey.getString("type")), !sortDescending);
            sortFields[i] = field;
        }
        return new Sort(sortFields);
    }

    private SortField.Type typeForSortField(String type) {
        switch (type) {
            case "String":
                return SortField.Type.STRING;
            case "Int":
                return SortField.Type.INT;
            case "Double":
                return SortField.Type.DOUBLE;
            case "Long":
                return SortField.Type.LONG;
        }
        return null;
    }
    
    private List<FacetRequest> convertToFacets(JsonArray facets) {
        if (facets == null)
            return null;
        List<FacetRequest> facetRequests = new ArrayList<FacetRequest>();
        for (int i = 0; i < facets.size(); i++) {
            JsonObject facet = facets.getJsonObject(i);
            FacetRequest fr = new FacetRequest(facet.getString("fieldname"), facet.getInt("maxTerms"));
            facetRequests.add(fr);
        }
        return facetRequests;
    }

    public Query convertToQuery(JsonObject query) {
        switch(query.getString("type")) {
            case "MatchAllDocsQuery":
                return new MatchAllDocsQuery();
            case "TermQuery":
                return createTermQuery(query.getJsonObject("term"));
            default:
                return null;
        }
    }

    private Query createTermQuery(JsonObject object) {
        return new TermQuery(new Term(object.getString("field"), object.getString("value")));
    }
    
    public static class FacetRequest {
        public String fieldname;
        public int maxTerms;
        public String[] path = new String[0];

        public FacetRequest(String fieldname, int maxTerms) {
            this.fieldname = fieldname;
            this.maxTerms = maxTerms;
        }
    }
   
}
