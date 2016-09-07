fun foo() {
    if (x) {
        if (x.y) {
            if (y) {

            }
            else {
                if (a) {

                }
                else if (b) {
                    if (q) {

                    }
                    else {
                        if (p) <caret>return
                    }
                }
                else {

                }
            }
        }
    }
}