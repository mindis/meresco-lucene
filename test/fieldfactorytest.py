## begin license ##
#
# "Meresco Lucene" is a set of components and tools to integrate Lucene (based on PyLucene) into Meresco
#
# Copyright (C) 2014 Seecr (Seek You Too B.V.) http://seecr.nl
# Copyright (C) 2014 Stichting Bibliotheek.nl (BNL) http://www.bibliotheek.nl
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

from seecr.test import SeecrTestCase
from meresco.lucene.fieldfactory import FieldFactory, createNoTermsFrequencyField
from org.apache.lucene.index import FieldInfo


class FieldFactoryTest(SeecrTestCase):

    def testDefault(self):
        factory = FieldFactory()
        field = factory.createField('__id__', 'id:1')
        self.assertFalse(field.fieldType().tokenized())
        self.assertTrue(field.fieldType().stored())
        self.assertTrue(field.fieldType().indexed())

    def testSpecificField(self):
        factory = FieldFactory()
        field = factory.createField('fieldname', 'value')
        self.assertTrue(field.fieldType().tokenized())
        def create(fieldname, value):
            return 'NEW FIELD'
        factory.register('fieldname', create)
        self.assertEquals('NEW FIELD', factory.createField('fieldname', 'value'))

    def testNoTermsFreqField(self):
        factory = FieldFactory()
        factory.register('fieldname', createNoTermsFrequencyField)
        field = factory.createField('fieldname', 'value')
        self.assertEquals(FieldInfo.IndexOptions.DOCS_ONLY, field.fieldType().indexOptions())