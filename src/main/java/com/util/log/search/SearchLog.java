package com.util.log.search;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;

/**
 * 
 * @author Vishnu Mishra
 *
 */
public class SearchLog {

	private Directory memoryIndex = null;
	private StandardAnalyzer analyzer = null;
	
	private static int FETCH_SIZE = 100000;
	
	public SearchLog(Directory memoryIndex, StandardAnalyzer analyzer) throws Exception {
		this.memoryIndex = memoryIndex;
		this.analyzer = analyzer;
	}

	public List<Document> search(String searchQuery, String logLevel) throws Exception {
		QueryParser queryParser = new QueryParser("Content", analyzer);
		Query query = queryParser.parse(searchQuery);
		
		// Search the index
		DirectoryReader directoryReader = DirectoryReader.open(memoryIndex);
		IndexSearcher searcher = new IndexSearcher(directoryReader);
		ScoreDoc[] hits;
		
		if(logLevel != "ALL") {
			QueryParser levelParser = new QueryParser("Level", analyzer);
			Query levelQuery = levelParser.parse(logLevel);
			
			// Combine the queries using a BooleanQuery
	        BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
	        booleanQuery.add(levelQuery, Occur.MUST);
	        booleanQuery.add(query, Occur.MUST);

	        // Execute the search
	        hits = searcher.search(booleanQuery.build(), FETCH_SIZE).scoreDocs; 
		} else {
			hits = searcher.search(query, FETCH_SIZE).scoreDocs;
		}

		List<Document> outputDocuments = new ArrayList<Document>();

		for (ScoreDoc hit : hits) {
			outputDocuments.add(searcher.doc(hit.doc));
		}

		directoryReader.close();
		
		return outputDocuments;
	}

}
