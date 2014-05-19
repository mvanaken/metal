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

package nl.minvenj.nfi.ddrx.data;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

import nl.minvenj.nfi.ddrx.expression.value.Value;

public class Environment {

    private final Deque<Value> _order;
    private final Deque<Integer> _marked;
    private final ByteStream _input;

    public Environment(ByteStream input) {
        _order = new ArrayDeque<Value>();
        _marked = new ArrayDeque<Integer>();
        _input = input;
    }

    public void put(Value value) {
        _order.push(value);
    }

    public Value get(String name) {
        for (Value v : _order) {
            if (v.getName().equals(name)) { return v; }
        }
        return null;
    }

    public Value current() {
        return _order.peek();
    }

    private void removeLast() {
        _order.pop();
    }

    public void mark() {
        _input.mark();
        _marked.add(_order.size());
    }

    public void clear() {
        _input.clear();
        _marked.pop();
    }

    public void reset() {
        _input.reset();
        final int reset = _order.size() - _marked.pop();
        for (int i = 0; i < reset; i++) {
            removeLast();
        }
    }

    public int read(byte[] data) throws IOException {
        return _input.read(data);
    }

}
