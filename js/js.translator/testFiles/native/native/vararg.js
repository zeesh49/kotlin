/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

function paramCount() {
    return arguments.length
}

function A(size, order) {
    this.size = size;
    A.checkOrder(order);
}

A.order = 0;
A.hasOrderProblem = false;
A.checkOrder = function (expectedOrder) {
    var curOrder = A.order++;
    A.hasOrderProblem = A.hasOrderProblem && curOrder === expectedOrder;
};

A.startNewTest = function () {
    A.hasOrderProblem = false;
    A.order = 0;
    return true;
};


A.prototype.test = function (order, dummy /*, args */) {
    A.checkOrder(order);
    return dummy === 1 && (arguments.length - 2) === this.size;
};

var b = {
    test : function (size /*, args */) {
        return (arguments.length - 1) === size;
    }
};

function testNativeVarargWithFunLit(/* args, f */) {
    var args = Array.prototype.slice.call(arguments, 0, arguments.length - 1);
    var f = arguments[arguments.length - 1];
    return typeof f === "function" && f(args);
}