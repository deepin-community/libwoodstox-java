package wstxtest.stream;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.ctc.wstx.api.WstxInputProperties;

public class TestAttributeLimits extends BaseStreamTest
{
    public void testMaxAttributesLimit() throws Exception
    {
        final int max = 100;
        XMLInputFactory factory = getNewInputFactory();
        factory.setProperty(WstxInputProperties.P_MAX_ATTRIBUTES_PER_ELEMENT, Integer.valueOf(50));
        Reader reader = new Reader() {
            StringReader sreader = new StringReader("<ns:element xmlns:ns=\"http://foo.com\"");
            int count;
            boolean done;
            @Override
            public int read(char[] cbuf, int off, int len) throws IOException {
                int i = sreader.read(cbuf, off, len);
                if (i == -1) {
                    if (count < max) {
                        sreader = new StringReader(" attribute" + count++ + "=\"foo\"");
                    } else if (!done) {
                        sreader = new StringReader("/>");
                        done = true;
                    }
                    i = sreader.read(cbuf, off, len);
                }
                return i;
            }
            @Override
            public void close() throws IOException { }
        };
        XMLStreamReader xmlreader = factory.createXMLStreamReader(reader);
        try {
            xmlreader.nextTag();
            fail("Should have failed");
        } catch (XMLStreamException ex) {
            verifyException(ex, "Attribute limit (50)");
        }
        reader.close();
    }

    // [woodstox-core#93]: should use stricter verification of max attr length
    public void testShorterAttribute() throws Exception
    {
        XMLInputFactory factory = getNewInputFactory();
        factory.setProperty(WstxInputProperties.P_MAX_ATTRIBUTE_SIZE, 4);

        // First: ok document
        XMLStreamReader r = factory.createXMLStreamReader(new StringReader(
                "<root attr='1234' other='ab' x='yz&amp;0' />"));
        assertTokenType(START_ELEMENT, r.next());
        assertEquals(3, r.getAttributeCount());
        assertTokenType(END_ELEMENT, r.next());
        r.close();

        // then not so much
        r = factory.createXMLStreamReader(new StringReader(
                "<root attr='1234' other='abcde'  />"));
        try {
            r.next();
            fail("Should not pass");
        } catch (XMLStreamException ex) {
            verifyException(ex, "Maximum attribute size limit (4)");
        }
    }

    public void testLongAttribute() throws Exception {
        final int max = 500;
        Reader reader = new Reader() {
            StringReader sreader = new StringReader("<ns:element xmlns:ns=\"http://foo.com\" blah=\"");
            int count;
            boolean done;
            @Override
            public int read(char[] cbuf, int off, int len) throws IOException {
                int i = sreader.read(cbuf, off, len);
                if (i == -1) {
                    if (count < max) {
                        sreader = new StringReader("          ");
                        count++;
                    } else if (!done) {
                        sreader = new StringReader("\"/>");
                        done = true;
                    }
                    i = sreader.read(cbuf, off, len);
                }
                return i;
            }
            @Override
            public void close() throws IOException { }
        };
        try {
            XMLInputFactory factory = getNewInputFactory();
            factory.setProperty(WstxInputProperties.P_MAX_ATTRIBUTE_SIZE, Integer.valueOf(100));
            XMLStreamReader xmlreader = factory.createXMLStreamReader(reader);
            while (xmlreader.next() != XMLStreamReader.END_DOCUMENT) {
            }
            fail("Should have failed");
        } catch (XMLStreamException ex) {
            verifyException(ex, "Maximum attribute size");
        }
        reader.close(); // never gets here
    }
}
