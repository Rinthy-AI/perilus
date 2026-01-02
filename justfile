ci: fmt clean test

test:
    sbt test

fmt:
    sbt scalafmtAll

clean:
    sbt clean