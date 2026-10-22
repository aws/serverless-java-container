#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT=$(cd "$(dirname "$0")/.." && pwd)
TEST_ROOT=$(mktemp -d)
trap 'rm -rf "$TEST_ROOT"' EXIT

FIXTURE="$TEST_ROOT/project"
BIN="$TEST_ROOT/bin"
mkdir -p "$FIXTURE" "$BIN"
cp "$REPO_ROOT/gha_build.sh" "$FIXTURE/gha_build.sh"
mkdir -p \
    "$FIXTURE/aws-serverless-java-container-core" \
    "$FIXTURE/aws-serverless-java-container-spring" \
    "$FIXTURE/aws-serverless-spring-archetype"

cat > "$BIN/mvn" <<'MAVN'
#!/usr/bin/env bash
set -euo pipefail
printf '%s\t%s\n' "$PWD" "$*" >> "$MAVEN_LOG"

if [[ "$PWD" == "$FIXTURE/aws-serverless-spring-archetype" && "$*" == "-q clean install" ]]; then
    exit 23
fi

if [[ "$*" == *archetype:generate* ]]; then
    mkdir -p "$PWD/spring-archetype-test"
    cat > "$PWD/spring-archetype-test/gradlew" <<'GRADLEW'
#!/usr/bin/env bash
exit 0
GRADLEW
    chmod +x "$PWD/spring-archetype-test/gradlew"
fi
MAVN
cat > "$BIN/gradle" <<'GRADLE'
#!/usr/bin/env bash
exit 0
GRADLE
chmod +x "$BIN/mvn" "$BIN/gradle"

export FIXTURE MAVEN_LOG="$TEST_ROOT/maven.log"
set +e
(cd "$FIXTURE" && PATH="$BIN:$PATH" bash ./gha_build.sh spring true false)
status=$?
set -e

if [[ $status -eq 0 ]]; then
    echo "gha_build.sh returned success after archetype mvn clean install failed" >&2
    exit 1
fi
if grep -q 'archetype:generate' "$MAVEN_LOG"; then
    echo "archetype generation ran after the archetype install failed" >&2
    exit 1
fi

echo "archetype install failure propagated before generation (exit $status)"
