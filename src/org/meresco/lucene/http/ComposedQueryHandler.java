/* begin license *
 *
 * "Meresco Lucene" is a set of components and tools to integrate Lucene (based on PyLucene) into Meresco
 *
 * Copyright (C) 2015-2016 Koninklijke Bibliotheek (KB) http://www.kb.nl
 * Copyright (C) 2015-2016 Seecr (Seek You Too B.V.) http://seecr.nl
 * Copyright (C) 2016 Stichting Kennisnet http://www.kennisnet.nl
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

package org.meresco.lucene.http;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.meresco.lucene.ComposedQuery;
import org.meresco.lucene.LuceneResponse;
import org.meresco.lucene.MultiLucene;
import org.meresco.lucene.OutOfMemoryShutdown;


public class ComposedQueryHandler extends AbstractMerescoLuceneHandler {
    private MultiLucene multiLucene;

    public ComposedQueryHandler(MultiLucene multiLucene, OutOfMemoryShutdown shutdown) {
        super(shutdown);
        this.multiLucene = multiLucene;
    }

    @Override
    public void doHandle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws Throwable {
        LuceneResponse luceneResponse = new LuceneResponse(0);
        ComposedQuery q;
        try {
            q = ComposedQuery.fromJsonString(request.getReader(), this.multiLucene.getQueryConverters());
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
        luceneResponse = this.multiLucene.executeComposedQuery(q);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.getWriter().write(luceneResponse.toJson().toString());
    }
}
