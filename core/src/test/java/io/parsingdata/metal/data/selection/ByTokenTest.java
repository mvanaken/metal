/*
 * Copyright 2013-2016 Netherlands Forensic Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.parsingdata.metal.data.selection;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import static io.parsingdata.metal.Shorthand.con;
import static io.parsingdata.metal.Shorthand.currentOffset;
import static io.parsingdata.metal.Shorthand.def;
import static io.parsingdata.metal.Shorthand.last;
import static io.parsingdata.metal.Shorthand.opt;
import static io.parsingdata.metal.Shorthand.ref;
import static io.parsingdata.metal.Shorthand.rep;
import static io.parsingdata.metal.Shorthand.repn;
import static io.parsingdata.metal.Shorthand.seq;
import static io.parsingdata.metal.Shorthand.sub;
import static io.parsingdata.metal.Shorthand.token;
import static io.parsingdata.metal.data.selection.ByName.getAllValues;
import static io.parsingdata.metal.data.selection.ByToken.get;
import static io.parsingdata.metal.data.selection.ByToken.getAll;
import static io.parsingdata.metal.data.selection.ByToken.getAllRoots;
import static io.parsingdata.metal.util.EncodingFactory.enc;
import static io.parsingdata.metal.util.EnvironmentFactory.stream;
import static io.parsingdata.metal.util.TokenDefinitions.any;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.parsingdata.metal.data.Environment;
import io.parsingdata.metal.data.ImmutableList;
import io.parsingdata.metal.data.ParseGraph;
import io.parsingdata.metal.data.ParseItem;
import io.parsingdata.metal.data.ParseValue;
import io.parsingdata.metal.data.transformation.Reversal;
import io.parsingdata.metal.encoding.Encoding;
import io.parsingdata.metal.expression.value.Value;
import io.parsingdata.metal.token.Token;

public class ByTokenTest {

    private static final Token DEF1 = def("value1", con(1));
    private static final Token DEF2 = def("value2", con(1));
    private static final Token TWO_BYTES = def("two", 2);
    private static final Token UNUSED_DEF = def("value3", con(1));

    private static final Token SIMPLE_SEQ = seq(DEF1, DEF2);
    private static final Token SEQ_REP = seq(DEF1, rep(DEF2));
    private static final Token SEQ_SUB = seq(DEF1, sub(TWO_BYTES, ref("value1")), DEF2, sub(TWO_BYTES, ref("value2")));
    private static final Token REPN_DEF2 = repn(DEF2, con(2));

    private static final Token MUT_REC_1 = seq(DEF1, new Token("", enc()) {

        @Override
        protected Optional<Environment> parseImpl(final String scope, final Environment environment, final Encoding encoding) throws IOException {
            return MUT_REC_2.parse(scope, environment, encoding);
        }
    });

    private static final Token MUT_REC_2 = seq(REPN_DEF2, opt(MUT_REC_1));
    
    private static final Token SELF_REC =
        seq("selfRec",
            DEF1,
            def("childCount", 1),
            repn(
                 token("selfRec"), last(ref("childCount"))));

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void nullCheck() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Argument definition may not be null");

        get(parseResultGraph(stream(0, 1, 2), SIMPLE_SEQ), null);
    }

    @Test
    public void findRootToken() {
        final ParseGraph graph = parseResultGraph(stream(0, 1, 2), SIMPLE_SEQ);
        final ParseItem item = get(graph, SIMPLE_SEQ);

        assertThat(item.getDefinition(), is(equalTo(SIMPLE_SEQ)));
    }

    @Test
    public void findNestedToken() {
        final ParseGraph graph = parseResultGraph(stream(0, 1, 2), SIMPLE_SEQ);
        final ParseItem item = get(graph, DEF1);

        assertThat(item.getDefinition(), is(equalTo(DEF1)));
    }

    @Test
    public void findUnusedToken() {
        final ParseGraph graph = parseResultGraph(stream(0, 1, 2), SIMPLE_SEQ);
        final ParseItem item = get(graph, UNUSED_DEF);

        assertThat(item, is(nullValue()));
    }

    @Test
    public void getAllNullCheck() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Argument definition may not be null");

        getAll(parseResultGraph(stream(0, 1, 2), SIMPLE_SEQ), null);
    }

    @Test
    public void getAllUnusedToken() {
        final ParseGraph graph = parseResultGraph(stream(0), SEQ_REP);
        final ImmutableList<ParseItem> items = getAll(graph, UNUSED_DEF);

        assertThat(items.size, is(equalTo(0L)));
    }

    @Test
    public void getAllNonePresent() {
        final ParseGraph graph = parseResultGraph(stream(0), SEQ_REP);
        final ImmutableList<ParseItem> items = getAll(graph, DEF2);

        assertThat(items.size, is(equalTo(0L)));
    }

    @Test
    public void getAllSingleDef() {
        final ParseGraph graph = parseResultGraph(stream(0, 1, 2, 3, 4, 5), SEQ_REP);
        final ImmutableList<ParseItem> items = getAll(graph, DEF1);

        assertThat(items.size, is(equalTo(1L)));
        assertThat(items.head.getDefinition(), is(equalTo(DEF1)));
    }

    @Test
    public void getAllRepDef() {
        final ParseGraph graph = parseResultGraph(stream(0, 1, 2, 3, 4, 5), SEQ_REP);
        final ImmutableList<ParseItem> items = getAll(graph, DEF2);

        assertThat(items.size, is(equalTo(5L)));
        assertThat(items.head.getDefinition(), is(equalTo(DEF2)));
    }

    @Test
    public void getAllRepSeq() {
        final ParseGraph graph = parseResultGraph(stream(0, 1, 2, 3, 4, 5), rep(SIMPLE_SEQ));
        final ImmutableList<ParseItem> def1Items = getAll(graph, DEF1);
        final ImmutableList<ParseItem> def2Items = getAll(graph, DEF2);

        assertThat(def1Items.size, is(equalTo(3L)));
        assertThat(def2Items.size, is(equalTo(3L)));

        assertThat(def1Items.head.getDefinition(), is(equalTo(DEF1)));
        assertThat(def2Items.head.getDefinition(), is(equalTo(DEF2)));

        assertThat(def1Items.tail.head.asValue().asNumeric().intValue(), is(equalTo(2)));
        assertThat(def2Items.tail.head.asValue().asNumeric().intValue(), is(equalTo(3)));
    }

    @Test
    public void getAllSub() {
        final ParseGraph graph = parseResultGraph(stream(4, 2, 2, 3, 4, 5), SEQ_SUB);
        final ImmutableList<ParseItem> items = getAll(graph, TWO_BYTES);

        assertThat(items.size, is(equalTo(2L)));
        assertThat(items.head.getDefinition(), is(equalTo(TWO_BYTES)));
        assertThat(items.head.asValue().getValue(), is(equalTo(new byte[]{2, 3})));
    }

    @Test
    public void getAllMutualRecursive() {
        final ParseGraph graph = parseResultGraph(stream(0, 1, 2, 3, 4, 5), MUT_REC_1);

        final ImmutableList<ParseItem> repItems = getAll(graph, REPN_DEF2);
        assertThat(repItems.size, is(equalTo(4L)));

        final ImmutableList<ParseItem> repRootItems = getAllRoots(graph, REPN_DEF2);
        assertThat(repRootItems.size, is(equalTo(2L)));

        final ImmutableList<ParseItem> recursiveItems = getAll(graph, MUT_REC_1);
        assertThat(recursiveItems.size, is(equalTo(4L)));

        final ImmutableList<ParseItem> recursiveRootItems = getAllRoots(graph, MUT_REC_1);
        assertThat(recursiveRootItems.size, is(equalTo(2L)));
    }

    @Test
    public void compareGetAllNameWithGetAllToken() {
        final ParseGraph graph = parseResultGraph(stream(0, 1, 2, 3, 4, 5), SEQ_REP);

        ImmutableList<Value> values = getAllValues(graph, "value2");
        ImmutableList<ParseItem> items = getAll(graph, DEF2);

        while (values.head != null) {
            assertThat(values.head, is(equalTo(items.head.asValue())));

            values = values.tail;
            items = items.tail;
        }
    }

    private ParseGraph parseResultGraph(final Environment env, final Token def) {
        try {
            return def.parse(env, enc()).get().order;
        }
        catch (final IOException e) {
            throw new AssertionError("Parsing failed", e);
        }
    }

    @Test
    public void getSubRef() throws IOException {
        final Token smallSub = sub(DEF2, last(ref("value1")));
        final Token extraSub = sub(any("x"), last(ref("value1")));
        final Token composition = seq(DEF1, smallSub, extraSub, smallSub, extraSub);
        final Optional<Environment> result = composition.parse(stream(0), enc());
        assertTrue(result.isPresent());
        final ImmutableList<ParseItem> items = getAll(result.get().order, DEF2);
        // should return the ParseGraph created by the Sub and the ParseReference that refers to the existing ParseItem
        assertEquals(2, items.size);
        assertTrue(items.head.isReference());
        assertTrue(items.tail.head.isValue());
    }

    private final Token smallSeq = seq(any("b"), any("c"));

    @Test
    public void getAllRootsSingle() throws IOException {
        final Token topSeq = seq(any("a"), smallSeq);
        final Optional<Environment> result = topSeq.parse(stream(1, 2, 3), enc());
        assertTrue(result.isPresent());
        final ImmutableList<ParseItem> seqItems = getAllRoots(result.get().order, smallSeq);
        assertEquals(1, seqItems.size);
        assertEquals(smallSeq, seqItems.head.getDefinition());
        final ParseValue c = seqItems.head.asGraph().head.asValue();
        assertEquals(3, c.asNumeric().intValue());
        assertEquals(2, c.slice.offset);
    }

    @Test
    public void getAllRootsMulti() throws IOException {
        final Token topSeq = seq(any("a"), smallSeq, smallSeq);
        final Optional<Environment> result = topSeq.parse(stream(1, 2, 3, 2, 3), enc());
        assertTrue(result.isPresent());
        final ImmutableList<ParseItem> seqItems = getAllRoots(result.get().order, smallSeq);
        assertEquals(2, seqItems.size);
        assertEquals(smallSeq, seqItems.head.getDefinition());
        assertEquals(smallSeq, seqItems.tail.head.getDefinition());
        final ParseValue c1 = seqItems.head.asGraph().head.asValue();
        assertEquals(3, c1.asNumeric().intValue());
        final ParseValue c2 = seqItems.tail.head.asGraph().head.asValue();
        assertEquals(3, c2.asNumeric().intValue());
        assertNotEquals(seqItems.head.asGraph().head, seqItems.tail.head.asGraph().head);
    }

    private Set<ParseItem> makeSet(final ImmutableList<ParseItem> seqs) {
        final Set<ParseItem> items = new HashSet<>();
        for (ImmutableList<ParseItem> current = seqs; current != null && !current.isEmpty(); current = current.tail) {
            items.add(current.head);
        }
        return items;
    }

    @Test
    public void getAllRootsMultiSub() throws IOException {
        final Optional<Environment> result = rep(seq(smallSeq, sub(smallSeq, currentOffset))).parse(stream(1, 2, 1, 2, 1, 2, 1, 2), enc());
                                                                                           /* 1: +--------+
                                                                                           /* 2:       +--------+
                                                                                           /* 3:             +--------+ */
        assertTrue(result.isPresent());
        final ImmutableList<ParseItem> seqItems = getAllRoots(result.get().order, smallSeq);
        assertEquals(6, seqItems.size); // Three regular and three subs.
        final Set<ParseItem> items = makeSet(seqItems);
        assertEquals(4, items.size()); // Check that there are two duplicates.
        for (final ParseItem item : items) {
            assertTrue(item.isGraph());
            assertEquals(2, item.asGraph().size);
            assertEquals(2, item.asGraph().head.asValue().asNumeric().intValue());
        }
    }

    private class CustomToken extends Token {

        public final Token token;

        public CustomToken() {
            super("", enc());
            token = seq(any("a"), opt(this));
        }

        @Override
        protected Optional<Environment> parseImpl(final String scope, final Environment environment, final Encoding encoding) throws IOException {
            return token.parse(scope, environment, encoding);
        }
    }

    @Test
    public void getAllRootsMultiSelf() throws IOException {
        final CustomToken customToken = new CustomToken();
        final Optional<Environment> result = customToken.parse(stream(1, 2, 3), enc());
        assertTrue(result.isPresent());
        final ImmutableList<ParseItem> seqItems = getAllRoots(result.get().order, customToken.token);
        assertEquals(3, seqItems.size);
        final Set<ParseItem> items = makeSet(seqItems);
        assertEquals(seqItems.size, items.size()); // Check that there are no duplicate results.
    }

    @Test
    public void getAllRootsEmpty() {
        assertEquals(0, getAllRoots(ParseGraph.EMPTY, any("a")).size);
        assertEquals(1, getAllRoots(ParseGraph.EMPTY, ParseGraph.NONE).size);
    }

    @Test
    public void getAllRootsOrderRepDef() throws IOException {
        final ParseGraph graph = parseResultGraph(stream(0, 1, 2), rep(DEF1));
        final ImmutableList<ParseItem> items = getAllRoots(graph, DEF1);
        assertThat(items.size, is(equalTo(3L)));
        assertThat(items.head.asValue().asNumeric().intValue(), is(equalTo(2)));
        assertThat(items.tail.head.asValue().asNumeric().intValue(), is(equalTo(1)));
        assertThat(items.tail.tail.head.asValue().asNumeric().intValue(), is(equalTo(0)));
    }

    @Test
    public void getAllRootsOrderSeqSub() {
        final ParseGraph graph = parseResultGraph(stream(4, 2, 2, 3, 4, 5), SEQ_SUB);
        final ImmutableList<ParseItem> items = getAllRoots(graph, TWO_BYTES);
        assertThat(items.size, is(equalTo(2L)));
        assertThat(items.head.asValue().asNumeric().intValue(), is(equalTo(0x0203)));
        assertThat(items.tail.head.asValue().asNumeric().intValue(), is(equalTo(0x0405)));
    }

    @Test
    public void getAllRootsOrderMutualRecursive() {
        final ParseGraph graph = parseResultGraph(stream(0, 1, 2, 3, 4, 5), MUT_REC_1);
        final ImmutableList<ParseItem> items = getAllRoots(graph, MUT_REC_1);
        assertThat(items.size, is(equalTo(2L)));

        // item.head is the MUT_REC_1 graph containing [3, 4, 5], with '3' having the DEF1 definition
        final ImmutableList<ParseItem> firstMutRecValues = getAll(items.head.asGraph(), DEF1);
        assertThat(firstMutRecValues.size, is(equalTo(1L)));
        assertThat(lastItem(firstMutRecValues).asValue().asNumeric().intValue(), is(equalTo(3)));

        // item.tail.head is the MUT_REC_1 graph containing [0, 1, 2, 3, 4, 5], with '0' and '3' having the DEF1 definition
        final ImmutableList<ParseItem> secondMutRecValues = getAll(items.tail.head.asGraph(), DEF1);
        assertThat(secondMutRecValues.size, is(equalTo(2L)));
        assertThat(lastItem(secondMutRecValues).asValue().asNumeric().intValue(), is(equalTo(0)));
    }

    @Test
    public void getAllRootsSelfRecursive() throws IOException {
        final ParseGraph graph = parseResultGraph(stream(0xA, 3, 0xB, 2, 0xC, 0, 0xD, 0, 0xE, 0, 0xF, 0), SELF_REC);
        ImmutableList<ParseItem> items = ByToken.getAllRoots(graph, SELF_REC);
        assertThat(items.size, is(equalTo(6L)));

        for (int value = 0xF; value >= 0xA; value--) {
            final ImmutableList<ParseItem> values = getAll(items.head.asGraph(), DEF1);
            assertThat(lastItem(values).asValue().asNumeric().intValue(), is(equalTo(value)));
            items = items.tail;
        }
    }

    private static <T> T lastItem(final ImmutableList<T> items) {
        return Reversal.reverse(items).head;
    }

}
