package com.util.log.search;

import java.io.Closeable;
import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;

/**
 * 
 * @author Vishnu Mishra
 *
 */
public class IndexLog implements Closeable {
	
	
	private IndexWriterConfig indexWriterConfig = null;
	private IndexWriter writer = null;
	
	public IndexLog(Directory memoryIndex, StandardAnalyzer analyzer) throws Exception {
		indexWriterConfig = new IndexWriterConfig(analyzer);
		writer = new IndexWriter(memoryIndex, indexWriterConfig);
	}

	public void index(Document document) throws Exception {
		writer.addDocument(document);
	}

	@Override
	public void close() throws IOException {
		writer.close();
	}
}
