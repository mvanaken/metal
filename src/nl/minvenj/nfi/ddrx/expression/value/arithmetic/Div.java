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

package nl.minvenj.nfi.ddrx.expression.value.arithmetic;

import java.math.BigInteger;

import nl.minvenj.nfi.ddrx.data.Environment;
import nl.minvenj.nfi.ddrx.encoding.Encoding;
import nl.minvenj.nfi.ddrx.expression.value.BinaryValueExpression;
import nl.minvenj.nfi.ddrx.expression.value.ConstantFactory;
import nl.minvenj.nfi.ddrx.expression.value.OptionalValue;
import nl.minvenj.nfi.ddrx.expression.value.Value;
import nl.minvenj.nfi.ddrx.expression.value.ValueExpression;

public class Div extends BinaryValueExpression {

    public Div(ValueExpression lop, ValueExpression rop) {
        super(lop, rop);
    }

    @Override
    public OptionalValue eval(Value lv, Value rv, Environment env, Encoding enc) {
        if (rv.asNumeric().equals(BigInteger.ZERO)) { return OptionalValue.empty(); }
        return OptionalValue.of(ConstantFactory.createFromNumeric(lv.asNumeric().divide(rv.asNumeric()), enc));
    }

}
