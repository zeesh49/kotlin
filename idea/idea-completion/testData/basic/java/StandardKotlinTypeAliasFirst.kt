fun some(e: IllegalArgumentException<caret>) {
}

// INVOCATION_COUNT: 2
// WITH_ORDER
// EXIST: { lookupString:"IllegalArgumentException", tailText: " (kotlin)", typeText:"IllegalArgumentException defined in kotlin" }
// EXIST: { lookupString:"IllegalArgumentException", tailText:" (java.lang)" }
