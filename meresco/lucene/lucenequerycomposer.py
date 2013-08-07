## begin license ##
#
# "Meresco Lucene" is a set of components and tools to integrate Lucene (based on PyLucene) into Meresco
#
# Copyright (C) 2013 Seecr (Seek You Too B.V.) http://seecr.nl
# Copyright (C) 2013 Stichting Bibliotheek.nl (BNL) http://www.bibliotheek.nl
#
# This file is part of "Meresco Lucene"
#
# "Meresco Lucene" is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# "Meresco Lucene" is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with "Meresco Lucene"; if not, write to the Free Software
# Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
#
## end license ##

from meresco.lucene import createAnalyzer
from meresco.lucene.utils import fieldType, LONGTYPE

from org.apache.lucene.search import TermQuery, BooleanClause, BooleanQuery, PrefixQuery, PhraseQuery, MatchAllDocsQuery, TermRangeQuery, NumericRangeQuery
from org.apache.lucene.index import Term
from org.apache.lucene.analysis.tokenattributes import CharTermAttribute
from java.io import StringReader

from cqlparser import CqlVisitor, UnsupportedCQL
from re import compile


def _analyzeToken(token):
    result = []
    reader = StringReader(unicode(token))
    stda = createAnalyzer()
    ts = stda.tokenStream("dummy field name", reader)
    termAtt = ts.addAttribute(CharTermAttribute.class_)
    try:
        ts.reset()
        while ts.incrementToken():
            result.append(termAtt.toString())
        ts.end()
    finally:
        ts.close()
    return result

prefixRegexp = compile(r'^([\w-]{2,})\*$') # pr*, prefix* ....

def _termOrPhraseQuery(index, termString):
    if fieldType(index) == LONGTYPE:
        return NumericRangeQuery.newLongRange(index, long(termString), long(termString), True, True)
    listOfTermStrings = _analyzeToken(termString.lower())
    if len(listOfTermStrings) == 1:
        if prefixRegexp.match(termString):
            return PrefixQuery(Term(index, listOfTermStrings[0]))
        return TermQuery(Term(index, listOfTermStrings[0]))
    result = PhraseQuery()
    for term in listOfTermStrings:
        result.add(Term(index, term))
    return result

def _termRangeQuery(index, relation, termString):
    field = index
    if '<' in relation:
        lowerTerm, upperTerm = None, termString
    else:
        lowerTerm, upperTerm = termString, None
    includeLower, includeUpper = relation == '>=', relation == '<='
    return TermRangeQuery.newStringRange(field, lowerTerm, upperTerm, includeLower, includeUpper)

LHS_OCCUR = {
    "AND": BooleanClause.Occur.MUST,
    "OR" : BooleanClause.Occur.SHOULD,
    "NOT": BooleanClause.Occur.MUST
}
RHS_OCCUR = {
    "AND": BooleanClause.Occur.MUST,
    "OR" : BooleanClause.Occur.SHOULD,
    "NOT": BooleanClause.Occur.MUST_NOT
}

class _Cql2LuceneQueryVisitor(CqlVisitor):
    def __init__(self, unqualifiedTermFields, node, isUntokenized):
        CqlVisitor.__init__(self, node)
        self._unqualifiedTermFields = unqualifiedTermFields
        self._isUntokenized = isUntokenized

    def visitSCOPED_CLAUSE(self, node):
        clause = CqlVisitor.visitSCOPED_CLAUSE(self, node)
        if len(clause) == 1:
            return clause[0]
        lhs, operator, rhs = clause
        query = BooleanQuery()
        query.add(lhs, LHS_OCCUR[operator])
        query.add(rhs, RHS_OCCUR[operator])
        return query

    def visitSEARCH_CLAUSE(self, node):
        # possible children:
        # CQL_QUERY
        # SEARCH_TERM
        # INDEX, RELATION, SEARCH_TERM
        firstChild = node.children[0].name
        results = CqlVisitor.visitSEARCH_CLAUSE(self, node)
        if firstChild == 'SEARCH_TERM':
            (unqualifiedRhs,) = results
            if unqualifiedRhs == '*':
                return MatchAllDocsQuery()
            if len(self._unqualifiedTermFields) == 1:
                fieldname, boost = self._unqualifiedTermFields[0]
                query = _termOrPhraseQuery(fieldname, unqualifiedRhs)
                query.setBoost(boost)
            else:
                query = BooleanQuery()
                for fieldname, boost in self._unqualifiedTermFields:
                    subQuery = _termOrPhraseQuery(fieldname, unqualifiedRhs)
                    subQuery.setBoost(boost)
                    query.add(subQuery, BooleanClause.Occur.SHOULD)
            return query
        elif firstChild == 'INDEX':
            (left, (relation, boost), right) = results
            if relation in ['==', 'exact'] or (relation == '=' and self._isUntokenized(left)):
                query = TermQuery(Term(left, right))
            elif relation == '=':
                query = _termOrPhraseQuery(left, right)
            elif relation in ['<','<=','>=','>']:
                query = _termRangeQuery(left, relation, right)
            else:
                raise UnsupportedCQL("'%s' not supported for the field '%s'" % (relation, left))

            query.setBoost(boost)
            return query
        else:
            ((query,),) = results
            return query

    def visitRELATION(self, node):
        results = CqlVisitor.visitRELATION(self, node)
        if len(results) == 1:
            relation = results[0]
            boost = 1.0
        else:
            (relation, (modifier, comparitor, value)) = results
            boost = float(value)
        return relation, boost

class LuceneQueryComposer(object):
    def __init__(self, unqualifiedTermFields, isUntokenizedMethod=None):
        self._unqualifiedTermFields = unqualifiedTermFields
        self._isUntokenized = (lambda name: False) if isUntokenizedMethod is None else isUntokenizedMethod

    def compose(self, ast):
        (result, ) = _Cql2LuceneQueryVisitor(unqualifiedTermFields=self._unqualifiedTermFields, node=ast, isUntokenized=self._isUntokenized).visit()
        return result

