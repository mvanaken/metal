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

package io.parsingdata.metal.token;

import static io.parsingdata.metal.Util.checkNotNull;
import static io.parsingdata.metal.data.ParseResult.failure;
import static io.parsingdata.metal.data.ParseResult.success;

import java.io.IOException;

import io.parsingdata.metal.data.Environment;
import io.parsingdata.metal.data.ImmutableList;
import io.parsingdata.metal.data.ParseResult;
import io.parsingdata.metal.data.ParseValue;
import io.parsingdata.metal.data.Slice;
import io.parsingdata.metal.encoding.Encoding;
import io.parsingdata.metal.expression.Expression;
import io.parsingdata.metal.expression.True;
import io.parsingdata.metal.expression.value.OptionalValue;
import io.parsingdata.metal.expression.value.ValueExpression;

/**
 * A {@link Token} that specifies a value to parse in the input.
 * <p>
 * A Def consists of a <code>size</code> (a {@link ValueExpression}, in bytes)
 * and a <code>predicate</code> (an {@link Expression}). The
 * <code>predicate</code> may be null.
 * <p>
 * Parsing will succeed if <code>size</code> evaluates to a single value, if
 * that many bytes are available in the input and if <code>predicate</code>
 * (if present) evaluates to <code>true</code>.
 *
 * @see Expression
 * @see Nod
 * @see ValueExpression
 */
public class Def extends Token {

    public final ValueExpression size;
    public final Expression predicate;

    public Def(final String name, final ValueExpression size, final Expression predicate, final Encoding encoding) {
        super(name, encoding);
        this.size = checkNotNull(size, "size");
        this.predicate = predicate == null ? new True() : predicate;
    }

    @Override
    protected ParseResult parseImpl(final String scope, final Environment environment, final Encoding encoding) throws IOException {
        final ImmutableList<OptionalValue> sizes = size.eval(environment, encoding);
        if (sizes.size != 1 || !sizes.head.isPresent()) {
            return failure(environment);
        }
        // TODO: Handle value expression results as BigInteger (#16)
        final int dataSize = sizes.head.get().asNumeric().intValue();
        // TODO: Consider whether zero is an acceptable size (#13)
        if (dataSize < 0) {
            return failure(environment);
        }
        final Slice slice = environment.slice(dataSize);
        if (slice.size != dataSize) {
            return failure(environment);
        }
        final Environment newEnvironment = environment.add(new ParseValue(scope, this, slice, encoding)).seek(environment.offset + dataSize);
        return predicate.eval(newEnvironment, encoding) ? success(newEnvironment) : failure(environment);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + makeNameFragment() + size + "," + predicate + ")";
    }

}
