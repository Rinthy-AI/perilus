ci: fmt clean test doc

test:
    sbt test

doc:
    typst compile -f svg doc/perilus.typ

fmt:
    sbt scalafmtAll

clean:
    sbt clean
