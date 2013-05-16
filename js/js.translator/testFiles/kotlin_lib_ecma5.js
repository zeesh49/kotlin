// Be aware — Google Chrome has serious issue — you can rewrite READ-ONLY property (if it is defined in prototype). Firefox and Safari work correct.
// Always test property access issues in Firefox, but not in Chrome.
var Kotlin = Object.create(null);

(function () {
    "use strict";

    // ecma5 is still sucks — concat doesn't accept arguments, but apply does, so, we just return arguments
    Kotlin.argumentsToArrayLike = function (args) {
      return args;
    };

    Kotlin.keys = Object.keys;

    Kotlin.isType = function (object, type) {
        if (object === null || object === undefined) {
            return false;
        }

        var proto = Object.getPrototypeOf(object);
        // todo test nested class
        //noinspection RedundantIfStatementJS
        if (proto == type.proto) {
            return true;
        }

        return false;
    };

    // as separated function to reduce scope
    function createConstructorWithoutInitializer() {
        return function $constructor() {
            return Object.create($constructor.proto);
        };
    }

    // as separated function to reduce scope
    function createConstructorWithInitializer() {
        return function $constructor() {
            var o = Object.create($constructor.proto);
            $constructor.initializer.apply(o, arguments);
            return o;
        };
    }

    function computeProto(bases, properties) {
        var proto = null;
        for (var i = 0, n = bases.length; i < n; i++) {
            var base = bases[i];
            var baseProto = base.proto;
            if (baseProto === null || base.properties === null) {
                continue;
            }

            if (proto === null) {
                proto = Object.create(baseProto, properties || undefined);
                continue;
            }
            Object.defineProperties(proto, base.properties);
            // todo test A -> B, C(->D) *properties from D is not yet added to proto*
        }

        return proto;
    }

    Kotlin.createTrait = function (bases, properties) {
        return createClass(bases, null, properties, false);
    };

    Kotlin.createClass = function (bases, initializer, properties) {
        // proto must be created for class even if it is not needed (requires for is operator)
        return createClass(bases, initializer === null ? function () {} : initializer, properties, true);
    };

    function computeProto2(bases, properties) {
        if (bases === null) {
            return null;
        }
        return Array.isArray(bases) ? computeProto(bases, properties) : bases.proto;
    }

    Kotlin.createObject = function (bases, initializer, properties) {
        var o = Object.create(computeProto2(bases, properties), properties || undefined);
        if (initializer !== null) {
            if (bases !== null) {
                Object.defineProperty(initializer, "baseInitializer", {value: Array.isArray(bases) ? bases[0].initializer : bases.initializer});
            }
            initializer.call(o);
        }
        Object.seal(o);
        return o;
    };

    function extendWithoutCheckOwn(obj1, obj2) {
        for (var v in obj2) {
            obj1[v] = obj2[v];
        }
        return obj1;
    }

    function createClass(bases, initializer, properties, isClass) {
        var proto;
        var baseInitializer;
        if (bases === null) {
            baseInitializer = null;
            proto = !isClass && properties === null ? null : extendWithoutCheckOwn(Object.create(null), properties);
        }
        else if (!Array.isArray(bases)) {
            baseInitializer = bases.initializer;
            proto = !isClass && properties === null ? bases.proto : extendWithoutCheckOwn(Object.create(bases.proto), properties);
        }
        else {
            // first is superclass, other are traits
            baseInitializer = bases[0].initializer;
            proto = computeProto(bases, properties);
            // all bases are traits without properties
            if (proto === null && isClass) {
                proto = Object.create(null, properties || undefined);
            }
        }

        var constructor;
        if (initializer != null) {
            constructor = createConstructorWithInitializer();
            constructor.initializer = initializer;
        }
        else {
            constructor = createConstructorWithoutInitializer();
        }
        constructor.proto = proto;
        constructor.properties = properties;
        if (isClass) {
            initializer.baseInitializer = baseInitializer;
        }

        return constructor;
    }

    Kotlin.definePackage = function (initializer, members) {
        //var definition = Object.create(null, members === null ? undefined : members);
        if (initializer == null) {
            return {value: members};
        }
        else {
            var getter = createPackageGetter(members, initializer);
            Object.freeze(getter);
            return {get: getter};
        }
    };

    function createPackageGetter(instance, initializer) {
        return function () {
            if (initializer !== null) {
                var tmp = initializer;
                initializer = null;
                tmp.call(instance);
                Object.seal(instance);
            }

            return instance;
        };
    }

    Kotlin.$new = function (f) {
        return f;
    };

    Kotlin.$createClass = function (parent, properties) {
        if (parent !== null && typeof (parent) != "function") {
            properties = parent;
            parent = null;
        }

        var initializer = null;
        var descriptors = properties ? {} : null;
        if (descriptors != null) {
            var ownPropertyNames = Object.getOwnPropertyNames(properties);
            for (var i = 0, n = ownPropertyNames.length; i < n; i++) {
                var name = ownPropertyNames[i];
                var value = properties[name];
                if (name == "initialize") {
                    initializer = value;
                }
                else if (name.indexOf("get_") === 0) {
                    // our std lib contains collision: hasNext property vs hasNext as function, we prefer function (actually, it does work)
                    var getterName = name.substring(4);
                    if (!descriptors.hasOwnProperty(getterName)) {
                        descriptors[getterName] = {get: value};
                        descriptors[name] = {value: value};
                    }
                }
                else if (name.indexOf("set_") === 0) {
                    descriptors[name.substring(4)] = {set: value};
                    // std lib code can refers to
                    descriptors[name] = {value: value};
                }
                else {
                    // we assume all our std lib functions are open
                    descriptors[name] = {value: value, writable: true};
                }
            }
        }

        return Kotlin.createClass(parent || null, initializer, descriptors);
    };

    Kotlin.defineModule = function (id, module) {
        if (id in Kotlin.modules) {
            throw Kotlin.$new(Kotlin.IllegalArgumentException)();
        }

        Object.freeze(module);
        Object.defineProperty(Kotlin.modules, id, {value: module});
    };
})();
